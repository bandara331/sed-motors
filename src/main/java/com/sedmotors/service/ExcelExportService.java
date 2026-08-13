package com.sedmotors.service;

import com.sedmotors.model.Booking;
import com.sedmotors.model.Invoice;
import com.sedmotors.model.Part;
import com.sedmotors.repository.BookingRepository;
import com.sedmotors.repository.InvoiceRepository;
import com.sedmotors.repository.PartRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExcelExportService {

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    public byte[] exportInventoryToExcel() throws IOException {
        List<Part> parts = partRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Part ID", "Part Name", "Category", "Stock Level", "Unit Price", "Total Asset Value", "Reorder Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // Data rows
            int rowIdx = 1;
            for (Part part : parts) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(part.getId());
                row.createCell(1).setCellValue(part.getName());
                row.createCell(2).setCellValue(part.getCategory());
                row.createCell(3).setCellValue(part.getStockQuantity());
                row.createCell(4).setCellValue(part.getPrice().doubleValue());
                row.createCell(5).setCellValue(part.getPrice().doubleValue() * part.getStockQuantity());
                row.createCell(6).setCellValue(part.getStockQuantity() < part.getReorderThreshold() ? "LOW STOCK" : "OK");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportBookingsToExcel() throws IOException {
        List<Booking> bookings = bookingRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bookings");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Booking ID", "Customer Name", "Contact", "Vehicle Plate", "Service Type", "Scheduled Date", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Booking b : bookings) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(b.getId());
                row.createCell(1).setCellValue(b.getCustomerName());
                row.createCell(2).setCellValue(b.getPhone());
                row.createCell(3).setCellValue(b.getVehicleDetails());
                row.createCell(4).setCellValue(b.getServiceType());
                row.createCell(5).setCellValue(b.getPreferredDate());
                row.createCell(6).setCellValue(b.getStatus());
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] exportInvoicesToExcel() throws IOException {
        List<Invoice> invoices = invoiceRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoices");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Invoice ID", "Customer", "Subtotal", "Tax", "Grand Total", "Payment Status", "Payment Method", "Date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (Invoice inv : invoices) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(inv.getId());
                row.createCell(1).setCellValue(inv.getWorkOrder() != null && inv.getWorkOrder().getBooking() != null ? inv.getWorkOrder().getBooking().getCustomerName() : "N/A");
                row.createCell(2).setCellValue(inv.getPartsTotal().add(inv.getLaborTotal()).doubleValue());
                row.createCell(3).setCellValue(inv.getTaxAmount().doubleValue());
                row.createCell(4).setCellValue(inv.getGrandTotal().doubleValue());
                row.createCell(5).setCellValue(inv.getPaymentStatus() != null ? inv.getPaymentStatus().name() : "");
                row.createCell(6).setCellValue(inv.getPaymentMethod() != null ? inv.getPaymentMethod().name() : "");
                row.createCell(7).setCellValue(inv.getIssuedAt() != null ? inv.getIssuedAt().toString() : "");
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
