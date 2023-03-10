package uk.nhs.england.fhirvalidator.awsProvider

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.MethodOutcome
import ca.uhn.fhir.rest.client.api.IGenericClient
import ca.uhn.fhir.rest.param.TokenParam
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.instance.model.api.IBaseBundle
import org.hl7.fhir.r4.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component


@Component
class AWSQuestionnaire(val awsClient: IGenericClient,
    //sqs: AmazonSQS?,
                       @Qualifier("R4") val ctx: FhirContext,
                       val awsAuditEvent: AWSAuditEvent
) {

    private val log = LoggerFactory.getLogger("FHIRAudit")

    public fun search(url : TokenParam?) : List<Questionnaire> {
        var resources = mutableListOf<Questionnaire>()
        var bundle: Bundle? = null
        var retry = 3
        while (retry > 0) {
            try {
                if (url != null) {
                    bundle = awsClient
                        .search<IBaseBundle>()
                        .forResource(Questionnaire::class.java)
                        .where(
                            Questionnaire.URL.matches().value(url.value)
                        )
                        .returnBundle(Bundle::class.java)
                        .execute()
                    break
                } else {
                    bundle = awsClient
                        .search<IBaseBundle>()
                        .forResource(Questionnaire::class.java)
                        .returnBundle(Bundle::class.java)
                        .execute()
                    break
                }
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        if (bundle!=null && bundle.hasEntry()) {
            for (entry in bundle.entry) {
                if (entry.hasResource() && entry.resource is Questionnaire) resources.add(entry.resource as Questionnaire)
            }
        }
        return resources
    }

    fun update(questionnaire: Questionnaire, theId: IdType): MethodOutcome? {
        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .update()
                    .resource(questionnaire)
                    .withId(theId)
                    .execute()
                val storedQuestionnaire = response.resource as Questionnaire
                val auditEvent = awsAuditEvent.createAudit(storedQuestionnaire, AuditEvent.AuditEventAction.U)
                awsAuditEvent.writeAWS(auditEvent)
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }

    fun create(questionnaire: Questionnaire): MethodOutcome? {
        if (questionnaire.hasCode() && !questionnaire.hasIdentifier()) {
            questionnaire.identifier.add(Identifier()
                .setSystem(questionnaire.codeFirstRep.system)
                .setValue(questionnaire.codeFirstRep.code))
        }
        if (questionnaire.hasCode() && questionnaire.codeFirstRep.hasCode() && !questionnaire.hasUrl()) {
            questionnaire.url = "https://example.fhir.nhs.uk/Questionnaire/"+questionnaire.codeFirstRep.code
        }
        if (!questionnaire.hasUrl()) throw UnprocessableEntityException("A Questionnaire.code or Questionnaire.url is required")
        val duplicateCheck = search(TokenParam().setValue(questionnaire.url))
        if (duplicateCheck.size>0) throw UnprocessableEntityException("A Questionnaire with this definition already exists.")

        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .create()
                    .resource(questionnaire)
                    .execute()
                val storedQuestionnaire = response.resource as Questionnaire
                val auditEvent = awsAuditEvent.createAudit(storedQuestionnaire, AuditEvent.AuditEventAction.C)
                awsAuditEvent.writeAWS(auditEvent)
                break
            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }

    fun delete(theId: IdType): MethodOutcome? {
        var response: MethodOutcome? = null
        var retry = 3
        while (retry > 0) {
            try {
                response = awsClient
                    .delete()
                    .resourceById(theId)
                    .execute()

                /*
                val auditEvent = awsAuditEvent.createAudit(storedQuestionnaire, AuditEvent.AuditEventAction.D)
                awsAuditEvent.writeAWS(auditEvent)
                */
                break

            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }

    /*
    fun read(theId: IdType): MethodOutcome? {
        var response = MethodOutcome()
        var retry = 3
        while (retry > 0) {
            try {
                val result = awsClient
                    .read()
                    .resource(Questionnaire::class.java)
                    .withId(theId)
                    .execute()
                if (result != null) {
                    response.setResource(result)
                }
                break

            } catch (ex: Exception) {
                // do nothing
                log.error(ex.message)
                retry--
                if (retry == 0) throw ex
            }
        }
        return response
    }*/
}
