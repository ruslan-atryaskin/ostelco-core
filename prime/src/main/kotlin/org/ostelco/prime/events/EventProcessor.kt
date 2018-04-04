package org.ostelco.prime.events

import com.google.common.base.Preconditions.checkNotNull
import com.lmax.disruptor.EventHandler
import io.dropwizard.lifecycle.Managed
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.logger
import org.ostelco.prime.storage.PurchaseRequestListener
import org.ostelco.prime.storage.Storage
import org.ostelco.prime.storage.StorageException
import org.ostelco.prime.storage.entities.NotATopupProductException
import org.ostelco.prime.storage.entities.Product
import org.ostelco.prime.storage.entities.PurchaseRequest
import org.ostelco.prime.storage.entities.TopUpProduct
import java.util.concurrent.atomic.AtomicBoolean

class EventProcessor(
        val storage: Storage,
        val ocsBalanceUpdater: OcsBalanceUpdater) : EventHandler<PrimeEvent>, Managed {

    private val LOG by logger()

    private val running = AtomicBoolean(false)

    @Throws(EventProcessorException::class)
    fun handlePurchaseRequest(pr: PurchaseRequest) {
        checkNotNull(pr)
        LOG.info("Handling purchase request = " + pr)

        validatePaymentToken(pr)

        val sku = getValidSku(pr)
        val msisdn = getValidMsisdn(pr)

        try {
            val topup = getValidTopUpProduct(pr, sku)
            handleTopupProduct(pr, msisdn, topup!!)
        } catch (ex: NotATopupProductException) {
            LOG.info("Ignoring non-topup purchase request " + pr)
        }

    }

    @Throws(EventProcessorException::class, NotATopupProductException::class)
    private fun getValidTopUpProduct(
            pr: PurchaseRequest,
            sku: String): TopUpProduct? {
        val product: Product? = storage.getProductForSku(sku)
        if (product == null || !product.isTopUpProject()) {
            throw EventProcessorException("Unknown product type, must be a topup product " + product.toString(), pr)
        }
        return product.asTopupProduct()
    }

    @Throws(EventProcessorException::class)
    private fun getValidMsisdn(pr: PurchaseRequest): String {
        return pr.msisdn
    }

    @Throws(EventProcessorException::class)
    private fun getValidSku(pr: PurchaseRequest): String {
        val sku = pr.sku

        if (!storage.isValidSKU(sku)) {
            throw EventProcessorException("Not a valid SKU: " + sku, pr)
        }

        return sku
    }

    @Throws(EventProcessorException::class)
    private fun validatePaymentToken(pr: PurchaseRequest) {
        val paymentToken = pr.paymentToken
    }

    @Throws(EventProcessorException::class)
    private fun handleTopupProduct(
            pr: PurchaseRequest,
            msisdn: String,
            topup: TopUpProduct) {
        try {
            LOG.info("Handling topup product = " + pr)
            storage.updateDisplayDatastructure(msisdn)
            storage.addRecordOfPurchaseByMsisdn(msisdn, pr.sku, pr.millisSinceEpoch)
            storage.removePurchaseRequestById(pr.id)
            ocsBalanceUpdater.updateBalance(msisdn, topup.noOfBytes)
        } catch (e: StorageException) {
            throw EventProcessorException(e)
        }

    }

    @Throws(EventProcessorException::class)
    private fun setRemainingByMsisdn(
            msisdn: String,
            noOfBytes: Long) {
        try {
            storage.setRemainingByMsisdn(msisdn, noOfBytes)
            storage.updateDisplayDatastructure(msisdn)
        } catch (e: StorageException) {
            throw EventProcessorException(e)
        }

    }

    override fun onEvent(
            event: PrimeEvent,
            sequence: Long,
            endOfBatch: Boolean) {


        // CREDIT_CONTROL_REQUEST is a high frequency operation. If we do want to
        // include data balance updates from this event type, then we can
        // skip 'switch' stmt since we will do 'setRemainingByMsisdn' for all cases.

        try {
            // XXX adding '+' prefix
            LOG.info("Updating data bundle balance for {} to {} bytes",
                    event.msisdn, event.bundleBytes)
            setRemainingByMsisdn("+" + event.msisdn!!, event.bundleBytes)
        } catch (e: Exception) {
            LOG.warn("Exception handling prime event in EventProcessor", e)
        }

        /*
        switch (event.getMessageType()) {

            // Continuous updates of data consumption. High frequency!
            case CREDIT_CONTROL_REQUEST:

            // response to my request, this is the new balance.
            case TOPUP_DATA_BUNDLE_BALANCE:

            // Return the amount to the balance. The typical use-case is that the
            // user has been allocated some balance but hasn't used it, and now the device\\
            // is switched off  so the data is returned to the backend/slow storage.

            case RETURN_UNUSED_DATA_BUCKET:
            case GET_DATA_BUNDLE_BALANCE:

                // XXX adding '+' prefix
                setRemainingByMsisdn("+" + event.getMsisdn(), event.getBundleBytes());
                break;
        }
        */
    }


    override fun start() {
        if (running.compareAndSet(false, true)) {
            addNewPurchaseRequestListener()
        }
    }

    private fun addNewPurchaseRequestListener() {
        storage.addPurchaseRequestListener(object : PurchaseRequestListener {
            override fun onPurchaseRequest(request: PurchaseRequest) {
                try {
                handlePurchaseRequest(request)
            } catch (e: EventProcessorException) {
                LOG.error("Could not handle purchase request " + request, e)
            }
            }
        })
    }

    override fun stop() {
        // Only for completeness, don't do anything special.
    }
}