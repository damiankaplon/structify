package io.structify.infrastructure.row.api

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.plugins.UnsupportedMediaTypeException
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper

suspend fun extractTextFromPdf(multipart: MultiPartData): String {
	var fileBytes: ByteArray = byteArrayOf()

	multipart.forEachPart { part ->
		when (part) {
			is PartData.FileItem -> {
				if (part.contentType?.toString()?.contains("application/pdf", ignoreCase = true) != true) {
					throw UnsupportedMediaTypeException(part.contentType ?: io.ktor.http.ContentType.Any)
				}
				fileBytes = part.provider().readRemaining().readByteArray()
			}
			else -> {}
		}
		part.dispose()
	}

	if (fileBytes.isEmpty()) {
		error("No file part in multipart request.")
	}

	val pdf = Loader.loadPDF(fileBytes)
	val textStripper = PDFTextStripper().apply {
		sortByPosition = true
	}
	return textStripper.getText(pdf)
}
