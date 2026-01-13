package io.structify.infrastructure.row.api

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.File
import java.util.UUID

fun createTempPdfFile(content: String): File {
	val tempFile = File.createTempFile("structify-test-pdf-${UUID.randomUUID()}", ".pdf")
	val document = PDDocument()
	val page = PDPage()
	document.addPage(page)
	val contentStream = PDPageContentStream(document, page)
	contentStream.beginText()
	contentStream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
	contentStream.showText(content)
	contentStream.endText()
	contentStream.close()
	document.save(tempFile)
	document.close()
	return tempFile
}
