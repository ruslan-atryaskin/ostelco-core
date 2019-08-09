package org.ostelco.prime.graphql

import com.fasterxml.jackson.core.type.TypeReference
import graphql.ExceptionWhileDataFetching
import graphql.GraphQLError
import graphql.GraphQLException
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.ostelco.prime.jsonmapper.objectMapper
import org.ostelco.prime.module.getResource
import org.ostelco.prime.storage.ClientDataSource
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import graphql.execution.DataFetcherResult
import org.ostelco.prime.apierror.ForbiddenError
import org.ostelco.prime.apierror.InternalServerError
import org.ostelco.prime.apierror.NotFoundError
import org.ostelco.prime.getLogger
import org.ostelco.prime.model.*
import org.ostelco.prime.paymentprocessor.core.ProductInfo

val clientDataSource by lazy { getResource<ClientDataSource>() }

// TODO: Return types and custom types where existing models doesn't do the job, should probably be moved to separate file
data class CreateCustomerPayload(val customer: Customer)
data class CreateApplicationTokenPayload(val applicationToken: ApplicationToken)
data class CreateJumioScanPayload(val jumioScan: ScanInformation)
data class Address(val address: String, val phoneNumber: String)
data class CreateAddressPayload(val address: Address)
data class CreateSimProfilePayload(val simProfile: SimProfile)
data class CreatePurchasePayload(val purchase: ProductInfo) // TODO: Should be a PurchaseRecord
data class NricInfo(val nric: String)
data class ValidateNricPayload(val nric: NricInfo)
data class ResendEmailPayload(val simProfile: SimProfile)

data class Customer(
        val id: String,
        val nickname: String,
        val contactEmail: String,
        val analyticsId: String,
        val referralId: String,
        val regions: Collection<RegionDetails>? = null
)

class CreateCustomerDataFetcher : DataFetcher<DataFetcherResult<CreateCustomerPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateCustomerPayload> {

        val result = DataFetcherResult.newResult<CreateCustomerPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val contactEmail = input.get("contactEmail")!!
        val nickname = input.get("nickname")!!
        val customer = Customer(
            contactEmail = contactEmail,
            nickname = nickname
        )

        return clientDataSource.addCustomer(identity = identity, customer = customer)
                .fold({
                    err.message(it.message)
                    err.extensions(mapOf("id" to it.id, "type" to it.type))
                    result.error(err.build())
                }, {
                    clientDataSource.getCustomer(identity).map{
                        CreateCustomerPayload(customer = Customer(id = it.id, nickname = it.nickname, contactEmail = it.contactEmail, analyticsId = it.analyticsId, referralId =  it.referralId))
                    }
                    .fold({
                        err.message(it.message)
                        err.extensions(mapOf("id" to it.id, "type" to it.type))
                        result.error(err.build())
                    }, {
                        result.data(it)
                    })
                }).build()
    }
}

class CreateJumioScanDataFetcher : DataFetcher<DataFetcherResult<CreateJumioScanPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateJumioScanPayload> {

        var result = DataFetcherResult.newResult<CreateJumioScanPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val regionCode = input.get("regionCode")!!

        return clientDataSource.createNewJumioKycScanId(identity = identity, regionCode = regionCode)
                .map {
                    CreateJumioScanPayload(jumioScan = it)
                }
                .fold({
                    err.message(it.message)
                    err.extensions(mapOf("id" to it.id, "type" to it.type))
                    result.error(err.build())
                }, {
                    result.data(it)
                }).build()
    }
}


class CustomerDataFetcher : DataFetcher<DataFetcherResult<Customer>>{
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Customer> {

        val result = DataFetcherResult.newResult<Customer>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getCustomer(identity).map{
            Customer(id = it.id, nickname = it.nickname, contactEmail = it.contactEmail, analyticsId = it.analyticsId, referralId = it.referralId)
        }.fold({
            err.message(it.message)
            err.extensions(mapOf("id" to it.id, "type" to it.type))
            result.error(err.build())
        }, {
            result.data(it)
        }).build();
    }
}

class DeleteCustomerDataFetcher : DataFetcher<DataFetcherResult<CreateCustomerPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateCustomerPayload> {

        val result = DataFetcherResult.newResult<CreateCustomerPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getCustomer(identity)
                .map {
                    customer -> clientDataSource.removeCustomer(identity)
                        .map {
                            result.data(CreateCustomerPayload(customer=Customer(id = customer.id, contactEmail = customer.contactEmail, nickname = customer.nickname, analyticsId = customer.analyticsId
                            , referralId = customer.referralId)))
                        }
                }
                .fold({
                    result.error(err.message(it.message).build())
                }, {
                    result
                }).build()
    }
}

class CreateApplicationTokenDataFetcher : DataFetcher<DataFetcherResult<CreateApplicationTokenPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateApplicationTokenPayload> {

        val result = DataFetcherResult.newResult<CreateApplicationTokenPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val applicationID = input.get("applicationID")!!
        val token = input.get("token")!!
        val tokenType = input.get("tokenType")!!
        val applicationToken = ApplicationToken(
                applicationID = applicationID,
                token = token,
                tokenType = tokenType
        )

        if (clientDataSource.addNotificationToken(customerId = identity.id, token = applicationToken)) {
            val payload = CreateApplicationTokenPayload(applicationToken = applicationToken)
            return result.data(payload).build()
        } else {
            err.message("Failed to store push token.")
            return result.error(err.build()).build()
        }
    }
}

class CreateSimProfileDataFetcher : DataFetcher<DataFetcherResult<CreateSimProfilePayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateSimProfilePayload> {

        val result = DataFetcherResult.newResult<CreateSimProfilePayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String,String>>("input")
        val regionCode = input.get("regionCode")!!
        val profileType = input.get("profileType")!!


        return clientDataSource.provisionSimProfile(
                identity = identity,
                regionCode = regionCode,
                profileType = profileType
        ).map{
            CreateSimProfilePayload(simProfile = it)
        }.fold({
            err.message(it.message)
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class SendEmailWithActivationQrCodeDataFetcher : DataFetcher<DataFetcherResult<ResendEmailPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<ResendEmailPayload> {

        val result = DataFetcherResult.newResult<ResendEmailPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val regionCode = input.get("regionCode")!!
        val iccId = input.get("iccId")!!

        return clientDataSource.sendEmailWithActivationQrCode(
                identity = identity,
                regionCode = regionCode,
                iccId = iccId
        ).map{
            ResendEmailPayload(simProfile=it)
        }.fold({
            err.message(it.message)
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class CreateAddressAndPhoneNumberDataFetcher : DataFetcher<DataFetcherResult<CreateAddressPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreateAddressPayload> {

        val result = DataFetcherResult.newResult<CreateAddressPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String, String>>("input")
        val address = input.get("address")!!
        val phoneNumber = input.get("phoneNumber")!!

        // TODO: Is address specific per region or global unique per user? If it's specific per region we need regionCode as well when we store it.
        return clientDataSource.saveAddressAndPhoneNumber(
                identity = identity,
                address = address,
                phoneNumber = phoneNumber
        ).map{
            CreateAddressPayload(address=Address(address = address, phoneNumber = phoneNumber))
        }.fold({
            err.message(it.message)
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class ValidateNRICDataFetcher : DataFetcher<DataFetcherResult<ValidateNricPayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<ValidateNricPayload> {

        val result = DataFetcherResult.newResult<ValidateNricPayload>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        val input = env.getArgument<Map<String,String>>("input")
        val nric = input.get("nric")!!

        return clientDataSource.checkNricFinIdUsingDave(
                identity = identity,
                nricFinId = nric
        ).map{
            ValidateNricPayload(nric = NricInfo(nric = nric))
        }.fold({
            err.message(it.message)
            result.error(err.build())
        }, {
            result.data(it)
        }).build()
    }
}

class AllRegionsDataFetcher : DataFetcher<DataFetcherResult<Collection<RegionDetails>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<RegionDetails>> {

        val result = DataFetcherResult.newResult<Collection<RegionDetails>>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getAllRegionDetails(identity = identity).map{
            it
        }.fold({
            err.message(it.message)
            result.error(GraphqlErrorBuilder.newError().message(it.message).build())
        }, {
            result.data(it)
        }).build()
    }
}

class AllProductsDataFetcher : DataFetcher<DataFetcherResult<Collection<Product>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<Product>> {

        val response = DataFetcherResult.newResult<Collection<Product>>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getProducts(identity = identity).map{
            it.values
        }.fold({
            err.message(it.message)
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

class AllBundlesDataFetcher : DataFetcher<DataFetcherResult<Collection<Bundle>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<Bundle>> {

        val response = DataFetcherResult.newResult<Collection<Bundle>>()
        val err = GraphqlErrorBuilder.newError()
        val identity = env.getContext<Identity>()

        return clientDataSource.getBundles(identity).fold({
            err.message(it.message)
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

/*
class PurchaseProductDataFetcher : DataFetcher<Map<String, Any>> {
    override fun get(env: DataFetchingEnvironment): Map<String, Any> {
        val identity = env.getContext<Identity>()
        val sku = env.getArgument<String>("sku")
        val sourceId = env.getArgument<String>("sourceId")

        return clientDataSource.purchaseProduct(
                identity = identity,
                sku = sku,
                sourceId = sourceId,
                saveCard = false
        ).map{
            clientDataSource.getProduct(identity = identity, sku = sku) // Return purchased product, or return updated bundle
        }.map{
            objectMapper.convertValue<Map<String, Any>>(it, object : TypeReference<Map<String, Any>>() {})
        }.fold({
            throw GraphQLException(it.message)
        }, {
            it
        }
        )
    }
}
*/

class AllPurchasesDataFetcher : DataFetcher<DataFetcherResult<Collection<PurchaseRecord>>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<Collection<PurchaseRecord>> {
        val identity = env.getContext<Identity>()
        val response = DataFetcherResult.newResult<Collection<PurchaseRecord>>()
        val err = GraphqlErrorBuilder.newError()
        return clientDataSource.getPurchaseRecords(identity = identity).map{
            it
        }.fold({
            err.message(it.message)
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

class CreatePurchaseDataFetcher : DataFetcher<DataFetcherResult<CreatePurchasePayload>> {
    override fun get(env: DataFetchingEnvironment): DataFetcherResult<CreatePurchasePayload> {
        val identity = env.getContext<Identity>()
        val input = env.getArgument<Map<String, String>>("input")
        val sku = input.get("sku")!!
        val sourceId = input.get("sourceId")!!
        val response = DataFetcherResult.newResult<CreatePurchasePayload>()
        val err = GraphqlErrorBuilder.newError()

        return clientDataSource.purchaseProduct(
                identity = identity,
                sku = sku,
                sourceId = sourceId,
                saveCard = false
        ).map{
            CreatePurchasePayload(purchase = it)
        }.fold({
            err.message(it.message)
            response.error(err.build())
        }, {
            response.data(it)
        }).build()
    }
}

// TODO: Not in use as of now, replaced by smaller DataFetchers, not sure if we need the selectionSet.contains pattern right now, since each section would be a separate call either way
class ContextDataFetcher : DataFetcher<Map<String, Any>> {

    override fun get(env: DataFetchingEnvironment): Map<String, Any>? {

        return env.getContext<Identity>()?.let { identity ->
            val map = mutableMapOf<String, Any>()
            if (env.selectionSet.contains("customer/*")) {
                clientDataSource.getCustomer(identity)
                        .map { customer ->
                            map.put("customer", objectMapper.convertValue(customer, object : TypeReference<Map<String, Any>>() {}))
                        }
            }
            if (env.selectionSet.contains("bundles/*")) {
                clientDataSource.getBundles(identity)
                        .map { bundles ->
                            map.put("bundles", bundles.map { bundle ->
                                objectMapper.convertValue<Map<String, Any>>(bundle, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("regions/*")) {
                val regionCode: String? = env.selectionSet.getField("regions").arguments["regionCode"]?.toString()
                if (regionCode.isNullOrBlank()) {
                    clientDataSource.getAllRegionDetails(identity)
                            .map { regions ->
                                map.put("regions", regions.map { region ->
                                    objectMapper.convertValue<Map<String, Any>>(region, object : TypeReference<Map<String, Any>>() {})
                                })
                            }
                } else {
                    clientDataSource.getRegionDetails(identity, regionCode.toLowerCase())
                            .map { region ->
                                map.put("regions",
                                        listOf(objectMapper.convertValue<Map<String, Any>>(region, object : TypeReference<Map<String, Any>>() {}))
                                )
                            }
                }
            }
            if (env.selectionSet.contains("products/*")) {
                clientDataSource.getProducts(identity)
                        .map { productsMap ->
                            map.put("products", productsMap.values.map { product ->
                                objectMapper.convertValue<Map<String, Any>>(product, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            if (env.selectionSet.contains("purchases/*")) {
                clientDataSource.getPurchaseRecords(identity)
                        .map { purchaseRecords ->
                            map.put("purchases", purchaseRecords.map { purchaseRecord ->
                                objectMapper.convertValue<Map<String, Any>>(purchaseRecord, object : TypeReference<Map<String, Any>>() {})
                            })
                        }
            }
            map
        }
    }
}

// TODO: To use this we need to throw an error inside the DataFetchers
class CustomDataFetcherExceptionHandler : DataFetcherExceptionHandler {

    override fun onException(handlerParameters: DataFetcherExceptionHandlerParameters): DataFetcherExceptionHandlerResult {
        val exception = handlerParameters.exception
        val sourceLocation = handlerParameters.sourceLocation
        val path = handlerParameters.path

        val error: GraphQLError = when(exception) {
            // is ValidationException -> ValidationDataFetchingGraphQLError(exception.constraintErrors, path, exception, sourceLocation)
            else -> ExceptionWhileDataFetching(path, exception, sourceLocation)
        }
        logger.warn(error.message, exception)
        return DataFetcherExceptionHandlerResult.newResult().error(error).build()
    }

    private val logger by getLogger()
}

