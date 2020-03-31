package io.github.erfangc.expectedreturns.internal

import io.github.erfangc.expectedreturns.ExpectedReturnsService.Companion.formatter
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.lang.RuntimeException
import java.time.LocalDate
import java.util.zip.ZipInputStream

@Service
class FrenchFamaFactorsParser(private val httpClient: HttpClient) {

    private val log = LoggerFactory.getLogger(FrenchFamaFactorsParser::class.java)

    private val url = "https://mba.tuck.dartmouth.edu/pages/faculty/ken.french/ftp/F-F_Research_Data_Factors_CSV.zip"

    fun factorLevels(): Map<LocalDate, FrenchFamaFactorLevel> {
        return try {
            val inputStream = httpClient
                    .execute(HttpGet(url))
                    .entity
                    .content
            val zis = ZipInputStream(inputStream)
            val nextEntry = zis.nextEntry
            val name = nextEntry.name
            log.info("Found entry: $name in French Fama file $url")
            val byteArray = zis.readAllBytes()
            val lines = ByteArrayInputStream(byteArray)
                    .bufferedReader()
                    .readLines()
                    .dropWhile { line ->
                        val cells = line.split(",").filter { !it.isBlank() }
                        !cells.containsAll(listOf("Mkt-RF", "SMB", "HML", "RF"))
                    }
            val factorLevels = lines
                    .drop(1)
                    .mapNotNull { line ->
                        val cells = line.split(",")
                        try {
                            val date = LocalDate.parse(cells[0], formatter)
                            val mktMinusRf = cells[1].trim().toDoubleOrNull()?.div(100.0)?:0.0
                            val smb = cells[2].trim().toDoubleOrNull()?.div(100.0)?:0.0
                            val hml = cells[3].trim().toDoubleOrNull()?.div(100.0)?:0.0
                            val rf = cells[4].trim().toDoubleOrNull()?.div(100.0)?:0.0
                            FrenchFamaFactorLevel(
                                    date = date.toString(),
                                    hml = hml,
                                    smb = smb,
                                    mktMinusRf = mktMinusRf,
                                    rf = rf
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .associateBy { LocalDate.parse(it.date) }
            zis.close()
            factorLevels
        } catch (e: Exception) {
            log.error("Unable to parse French-Fama Factors", e)
            throw RuntimeException(e)
        }
    }

}