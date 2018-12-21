package org.ostelco.prime.ocs.core

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.FinalUnitAction
import org.ostelco.ocs.api.FinalUnitIndication
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ReportingReason
import org.ostelco.ocs.api.ServiceUnit
import org.ostelco.prime.module.getResource
import org.ostelco.prime.ocs.analytics.AnalyticsReporter
import org.ostelco.prime.ocs.consumption.OcsAsyncRequestConsumer
import org.ostelco.prime.ocs.notifications.Notifications
import org.ostelco.prime.storage.ClientDataSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object OnlineCharging : OcsAsyncRequestConsumer {

    private val threadContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val ccaStreamMap = ConcurrentHashMap<String, StreamObserver<CreditControlAnswerInfo>>()
    private val activateStreamMap = ConcurrentHashMap<String, StreamObserver<ActivateResponse>>()

    private val storage: ClientDataSource = getResource()

    override fun putCreditControlClient(
            streamId: String,
            creditControlAnswer: StreamObserver<CreditControlAnswerInfo>) {

        ccaStreamMap[streamId] = creditControlAnswer
    }

    override fun updateActivateResponse(streamId: String, activateResponse: StreamObserver<ActivateResponse>) {
        activateStreamMap[streamId] = activateResponse
    }

    override fun deleteCreditControlClient(streamId: String) {
        ccaStreamMap.remove(streamId)
    }

    override fun creditControlRequestEvent(streamId: String, request: CreditControlRequestInfo) {

        val msisdn = request.msisdn

        if (msisdn != null) {

            CoroutineScope(threadContext).launch {

                val response = CreditControlAnswerInfo.newBuilder()
                        .setRequestId(request.requestId)
                        .setMsisdn(msisdn)

                if (request.msccCount > 0) {
                    val mscc = request.getMscc(0)
                    val requested = mscc?.requested?.totalOctets ?: 0
                    val used = mscc?.used?.totalOctets ?: 0

                    val responseMscc = MultipleServiceCreditControl
                            .newBuilder(mscc)
                            .setValidityTime(86400)

                    val (granted, balance) = storage.consume(msisdn, used, requested).fold({ Pair(0L, null) }, { it })

                    val grantedTotalOctets = if (mscc.reportingReason != ReportingReason.FINAL
                            && mscc.requested.totalOctets > 0) {

                        if (granted < mscc.requested.totalOctets) {
                            responseMscc.finalUnitIndication = FinalUnitIndication.newBuilder()
                                    .setFinalUnitAction(FinalUnitAction.TERMINATE)
                                    .setIsSet(true)
                                    .build()
                        }

                        granted

                    } else {
                        // Use -1 to indicate no granted service unit should be included in the answer
                        -1
                    }

                    responseMscc.granted = ServiceUnit.newBuilder().setTotalOctets(grantedTotalOctets).build()

                    if (balance != null) {
                        launch {
                            AnalyticsReporter.report(
                                    request = request,
                                    bundleBytes = balance)
                        }

                        launch {
                            Notifications.lowBalanceAlert(
                                    msisdn = msisdn,
                                    reserved = granted,
                                    balance = balance)
                        }
                    }
                    response.addMscc(responseMscc)
                }

                ccaStreamMap[streamId]?.onNext(response.build())
            }
        }
    }
}