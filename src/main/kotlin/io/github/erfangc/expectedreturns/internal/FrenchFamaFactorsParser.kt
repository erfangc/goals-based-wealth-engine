package io.github.erfangc.expectedreturns.internal

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.PutItemRequest
import io.github.erfangc.expectedreturns.ExpectedReturnsService.Companion.formatter
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream

@Service
class FrenchFamaFactorsParser(
        private val ddb: AmazonDynamoDB,
        private val httpClient: HttpClient
) {

    private val log = LoggerFactory.getLogger(FrenchFamaFactorsParser::class.java)

    fun run() {
        // this should be a ZipFile
        try {
            val inputStream = httpClient
                    .execute(HttpGet("https://mba.tuck.dartmouth.edu/pages/faculty/ken.french/ftp/F-F_Research_Data_Factors_CSV.zip"))
                    .entity
                    .content
            val zis = ZipInputStream(inputStream)
            val nextEntry = zis.nextEntry
            val name = nextEntry.name
            log.info("Found entry: $name")
            val byteArray = zis.readAllBytes()
            val lines = ByteArrayInputStream(byteArray)
                    .bufferedReader()
                    .readLines()
                    .dropWhile { line ->
                        val cells = line.split(",").filter { !it.isBlank() }
                        !cells.containsAll(listOf("Mkt-RF", "SMB", "HML", "RF"))
                    }
            lines
                    .drop(1)
                    .forEach { line ->
                        val cells = line.split(",")
                        if (cells.size == 5) {
                            val date = LocalDate.parse(cells[0], formatter)
                            val mktMinusRf = cells[1].trim()
                            val smb = cells[2].trim()
                            val hml = cells[3].trim()
                            val rf = cells[4].trim()
                            try {
                                val putItemRequest = PutItemRequest(
                                        "factor-set-levels",
                                        mapOf(
                                                "id" to AttributeValue("french-fama-3-factor"),
                                                "date" to AttributeValue(date.toString()),
                                                "mktMinusRf" to AttributeValue().withN(mktMinusRf),
                                                "smb" to AttributeValue().withN(smb),
                                                "hml" to AttributeValue().withN(hml),
                                                "rf" to AttributeValue().withN(rf)
                                        )
                                )
                                ddb.putItem(putItemRequest)
                            } catch (e: Exception) {
                                log.error("Unable to write factor level into database", e)
                            }
                        }
                    }
            log.info("Found $lines lines in $name} and loaded them into th database")
            zis.close()
        } catch (e: Exception) {
            log.error("Unable to parse French Fama Factors", e)
        }
    }
}