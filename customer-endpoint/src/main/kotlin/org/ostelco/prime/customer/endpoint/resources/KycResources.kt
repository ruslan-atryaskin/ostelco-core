package org.ostelco.prime.customer.endpoint.resources

import io.dropwizard.auth.Auth
import org.ostelco.prime.auth.AccessTokenPrincipal
import org.ostelco.prime.customer.endpoint.store.SubscriberDAO
import org.ostelco.prime.jsonmapper.asJson
import org.ostelco.prime.model.Identity
import javax.validation.constraints.NotNull
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * Generic [KycResource] which has eKYC API common for all the countries.
 *
 * If there are any country specific API, then have a country specific KYC Resource which will inherit common APIs and
 * add country specific APIs.
 *
 * For now we have simple case, which can be handled by inheritance.  But, if it starts to get messy, then replace it
 * with Strategy pattern and use composition instead of inheritance.
 *
 */
open class KycResource(private val regionCode: String, private val dao: SubscriberDAO) {

    @Path("/jumio")
    fun jumioResource(): JumioKycResource {
        return JumioKycResource(regionCode = regionCode, dao = dao)
    }
}

/**
 * [SingaporeKycResource] uses [JumioKycResource] via parent class [KycResource].
 * It has Singapore specific eKYC APIs.
 *
 */
class SingaporeKycResource(private val dao: SubscriberDAO): KycResource(regionCode = "sg", dao = dao) {

    @GET
    @Path("/myInfo/{authorisationCode}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCustomerMyInfoData(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("authorisationCode")
            authorisationCode: String): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getCustomerMyInfoData(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                authorisationCode = authorisationCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { personalData -> Response.status(Response.Status.OK).entity(personalData) })
                .build()
    }

    @POST
    @Path("/dave")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkIdNumberUsingDave(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("authorisationCode")
            authorisationCode: String): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return Response.status(Response.Status.CREATED).build()
    }

    @POST
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    fun saveProfile(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("authorisationCode")
            authorisationCode: String): Response {

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return Response.status(Response.Status.CREATED).build()
    }
}

class JumioKycResource(private val regionCode: String, private val dao: SubscriberDAO) {

    @POST
    @Path("/scans")
    @Produces(MediaType.APPLICATION_JSON)
    fun newEKYCScanId(
            @Auth token: AccessTokenPrincipal?): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.createNewJumioKycScanId(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                regionCode = regionCode)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { scanInformation -> Response.status(Response.Status.CREATED).entity(scanInformation) })
                .build()
    }

    @GET
    @Path("/scans/{scanId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    fun getScanStatus(
            @Auth token: AccessTokenPrincipal?,
            @NotNull
            @PathParam("scanId")
            scanId: String
    ): Response {
        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .build()
        }

        return dao.getScanInformation(
                identity = Identity(id = token.name, type = "EMAIL", provider = token.provider),
                scanId = scanId)
                .fold(
                        { apiError -> Response.status(apiError.status).entity(asJson(apiError)) },
                        { scanInformation -> Response.status(Response.Status.OK).entity(scanInformation) })
                .build()
    }
}