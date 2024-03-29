package uk.nhs.england.fhirvalidator

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.IValidationSupport
import ca.uhn.fhir.rest.api.EncodingEnum
import ca.uhn.fhir.rest.server.RestfulServer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import uk.nhs.england.fhirvalidator.providerSTU3.ConversionProviderSTU3
import uk.nhs.england.fhirvalidator.providerSTU3.ValidateProviderSTU3
import java.util.*
import jakarta.servlet.annotation.WebServlet

@ConditionalOnProperty(prefix = "services", name = ["STU3"])
@WebServlet("/FHIR/STU3/*", loadOnStartup = 1)
class FHIRSTU3RestfulServer(
    @Qualifier("STU3") fhirContext: FhirContext,
    private val validateSTU3Provider: ValidateProviderSTU3,
    val conversionProviderSTU3: ConversionProviderSTU3,
    @Qualifier("SupportChain") private val supportChain: IValidationSupport

    ) : RestfulServer(fhirContext) {

    override fun initialize() {
        super.initialize()

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        registerProvider(validateSTU3Provider)
        registerProvider(conversionProviderSTU3)

        isDefaultPrettyPrint = true
        defaultResponseEncoding = EncodingEnum.JSON
    }
}
