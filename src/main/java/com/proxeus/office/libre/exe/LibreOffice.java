package com.proxeus.office.libre.exe;

import com.proxeus.office.libre.conn.BootstrapConnector;
import com.proxeus.office.libre.conn.BootstrapSocketConnector;
import com.proxeus.office.libre.conn.OOInputStream;
import com.proxeus.office.libre.conn.OOOutputStream;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XEmbeddedObjectSupplier;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.XComponent;
import com.sun.star.sheet.CellFlags;
import com.sun.star.sheet.XCalculatable;
import com.sun.star.sheet.XCellRangesQuery;
import com.sun.star.sheet.XSheetCellRanges;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XCell;
import com.sun.star.text.XDocumentIndex;
import com.sun.star.text.XDocumentIndexesSupplier;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextEmbeddedObjectsSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XCloseable;
import com.sun.star.util.XRefreshable;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.sun.star.uno.UnoRuntime.queryInterface;

/**
 * The LibreOffice class defines an executable of LibreOffice.
 * This executable is mainly used to convert to PDF.
 * Before it converts it to PDF, it ensures that the table of contents and spread sheet formulas are updated.
 */
public class LibreOffice implements Closeable {
    private String exeDir;
    private BootstrapConnector con;
    private long lastReconnect = 0;
    private final static String OUT_URL = "private:stream";

    public LibreOffice(String exeDir) {
        this.exeDir = exeDir;
        this.lastReconnect = -1;
    }

    public LibreOffice(String exeDir, boolean connect) throws java.lang.Exception {
        this.exeDir = exeDir;
        reconnect(System.currentTimeMillis());
    }

    public String Convert(File src, File dst, String format) throws java.lang.Exception {
        LibreOfficeFormat lof = LibreOfficeFormat.get(format);
        exportDocument(src, dst, lof);
        return lof.contentType;
    }

    /**
     * @param reconnectStamp time stamp of the attempt
     * @return true if reconnected
     * @throws java.lang.Exception connection error
     */
    public boolean reconnect(long reconnectStamp) throws java.lang.Exception {
        if (needToReconnect(reconnectStamp)) {
            if (con != null) {
                con.disconnect();
            }
            //BootstrapPipeConnector c = new BootstrapPipeConnector(exeDir);
            //c.connect();
            BootstrapSocketConnector c = new BootstrapSocketConnector(exeDir);
            c.connect();
            con = c;
            lastReconnect = reconnectStamp;
            return true;
        }
        return false;
    }

    public boolean needToReconnect(long reconnectStamp) {
        return lastReconnect < reconnectStamp;
    }

    private void exportDocument(File src, File dst, LibreOfficeFormat outputFormat) throws java.lang.Exception {
        try {
            //InputStream input = new FileInputStream(src);
            //OOInputStream ooInputStream = new OOInputStream(input);
            String sUrl = src.toURI().toString();
            System.out.println("DEBUG SURL: " + sUrl);
            XComponent oDocToStore = null;
            try {
                oDocToStore = con.getCompLoader().loadComponentFromURL(sUrl, "_blank", 0, createProps(
                        p("Hidden", Boolean.TRUE),
                        p("RepairPackage", Boolean.TRUE),
                        p("Overwrite", Boolean.FALSE),
                        //p("InputStream", ooInputStream),
                        p("FilterName", "writer8"),
                        p("ReadOnly", Boolean.FALSE)
                ));
                if (oDocToStore == null) {
                    lastReconnect = -1;//force reconnect
                    throw new ExceptionInInitializerError("No doc to store. No Please try again later.");
                }
            } catch (NullPointerException eee) {
                throw new ExceptionInInitializerError("Internal error.  Please try again later.");
            }

            try {
                refreshTableOfContent(oDocToStore);
            } catch (DisposedException e) {
                // looks like we have lost the connection to libre
                // throw it up so the connection can be established again
                throw e;
            } catch (java.lang.Exception e) {
                System.err.println("Error updating TOC " + e.getMessage());
            }
            try {
                recalculateEmbeddedSheetObjects(oDocToStore);
            } catch (DisposedException e) {
                // looks like we have lost the connection to libre
                // throw it up so the connection can be established again
                throw e;
            } catch (java.lang.Exception e) {
                System.err.println("Error recalculating formulas in embedded sheet " + e.getMessage());
            }
            XStorable xStorable = queryInterface(XStorable.class, oDocToStore);
            if (xStorable == null) {
                lastReconnect = -1;
                throw new ExceptionInInitializerError();
            }
            //OutputStream output = new FileOutputStream(dst);
            //OOOutputStream out = new OOOutputStream(output);
            // Storing and converting the document
            File tmp = null;
            if (src == dst) {
                tmp = File.createTempFile("libre_dst", ".tmp");
                sUrl = tmp.toURI().toString();
            } else {
                sUrl = dst.toURI().toString();
            }
            xStorable.storeToURL(sUrl, createProps(
                    p("Overwrite", Boolean.TRUE),
                    p("FilterName", outputFormat.filterName),
                    p("Hidden", Boolean.TRUE)
                    //p("OutputStream", out)
            ));

            // Closing the converted document. Use XCloseable.close if the
            // interface is supported, otherwise use XComponent.dispose
            XCloseable xCloseable = queryInterface(XCloseable.class, xStorable);
            if (xCloseable != null) {
                xCloseable.close(false);
            } else {
                XComponent xComp = queryInterface(XComponent.class, xStorable);
                xComp.dispose();
            }
            if (tmp != null) {
                Files.move(tmp.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            //output.flush();
            //output.close();
        } catch (DisposedException e) {
            lastReconnect = -1;//force reconnect
            throw new ExceptionInInitializerError("Please try again later.");
        }
    }

    public void repairDocument(File src, File dst) throws java.lang.Exception {
        InputStream input = new FileInputStream(src);
        OutputStream output = null;
        // Composing the URL by replacing all backslashs
        //String sUrl = "file:///" + temp.getAbsolutePath().replace('\\', '/');
        OOInputStream ooInputStream = new OOInputStream(input);
        // Open the corrupted document
        XComponent xDocument = con.getCompLoader().loadComponentFromURL(OUT_URL, "_blank", 0, createProps(
                p("Hidden", Boolean.TRUE),
                p("RepairPackage", Boolean.TRUE),
                p("FilterName", "writer8"),
                p("InputStream", ooInputStream),
                p("ReadOnly", Boolean.FALSE),
                p("UpdateDocMode", Short.valueOf("3"))
        ));
        // Prepare Url for the output directory
        output = new FileOutputStream(dst);
        OOOutputStream out = new OOOutputStream(output);
        try {
            refreshTableOfContent(xDocument);
        } catch (java.lang.Exception e) {
            System.err.println("Error updating TOC " + e.getMessage());
        }
        try {
            recalculateEmbeddedSheetObjects(xDocument);
        } catch (java.lang.Exception e) {
            System.err.println("Error recalculating formulas in embedded sheet " + e.getMessage());
        }
        // Getting an object that will offer a simple way to store
        // a document to a URL.
        XStorable xStorable = queryInterface(XStorable.class, xDocument);

        // Storing and converting the document
        xStorable.storeToURL(OUT_URL, createProps(
                p("OutputStream", out),
                p("FilterName", "writer8")
        ));

        // Closing the converted document. Use XCloseable.close if the
        // interface is supported, otherwise use XComponent.dispose
        XCloseable xCloseable = queryInterface(XCloseable.class, xStorable);
        if (xCloseable != null) {
            xCloseable.close(false);
        } else {
            XComponent xComp = queryInterface(XComponent.class, xStorable);
            xComp.dispose();
        }
        output.flush();
        output.close();
    }

    private void recalculateEmbeddedSheetObjects(XComponent xDoc) throws java.lang.Exception {
        XTextEmbeddedObjectsSupplier xTEOS = queryInterface(XTextEmbeddedObjectsSupplier.class, xDoc);
        if (xTEOS == null) return;
        XNameAccess oEmObj = xTEOS.getEmbeddedObjects();
        for (String objectName : oEmObj.getElementNames()) {
            try {
                XTextContent oObj = queryInterface(XTextContent.class, oEmObj.getByName(objectName));
                XEmbeddedObjectSupplier embOb = queryInterface(XEmbeddedObjectSupplier.class, oObj);
                XComponent comp = embOb.getEmbeddedObject();
                XCalculatable abc = queryInterface(XCalculatable.class, comp);
                XSpreadsheetDocument sss = queryInterface(XSpreadsheetDocument.class, comp);
                XSpreadsheets xSpreadsheets = sss.getSheets();
                XIndexAccess xIndexAccess = queryInterface(XIndexAccess.class, xSpreadsheets);
                for (int i = 0; i < xIndexAccess.getCount(); ++i) {
                    XSpreadsheet xSpreadsheet = queryInterface(XSpreadsheet.class, xIndexAccess.getByIndex(i));
                    XCellRangesQuery xCellQuery = UnoRuntime.queryInterface(XCellRangesQuery.class, xSpreadsheet);
                    XSheetCellRanges xFormulaCells = xCellQuery.queryContentCells((short) CellFlags.STRING);
                    XEnumerationAccess xFormulas = xFormulaCells.getCells();
                    XEnumeration xFormulaEnum = xFormulas.createEnumeration();
                    while (xFormulaEnum.hasMoreElements()) {
                        Object formulaCell = xFormulaEnum.nextElement();
                        XCell xCell = queryInterface(XCell.class, formulaCell);
                        String val = xCell.getFormula();
                        if (val.length() > 1) {
                            val = xCell.getFormula().substring(1);
                            if (isNumber(val)) {
                                xCell.setFormula(val);
                            }
                        }
                    }
                }
                abc.calculateAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isNumber(CharSequence self) {
        try {
            new BigDecimal(self.toString().trim());
            return true;
        } catch (NumberFormatException var2) {
            return false;
        }
    }

    /**
     * Refresh the table of content
     */
    protected void refreshTableOfContent(XComponent xDoc) throws java.lang.Exception {
        XDocumentIndexesSupplier indexSupplier = queryInterface(XDocumentIndexesSupplier.class, xDoc);
        if (indexSupplier == null) {
            return;
        }
        XIndexAccess indexAccess = indexSupplier.getDocumentIndexes();
        if (indexAccess == null) {
            return;
        }
        if (indexAccess.getCount() > 0) {
            Object objectIndex = indexAccess.getByIndex(0);
            if (objectIndex != null) {
                (queryInterface(XRefreshable.class, xDoc)).refresh();
                XDocumentIndex index = queryInterface(XDocumentIndex.class, objectIndex);
                index.update();
                (queryInterface(XRefreshable.class, xDoc)).refresh();
            }
        }
        XDocumentIndexesSupplier xDocumentIndexesSupplier = queryInterface(XDocumentIndexesSupplier.class, xDoc);
        XIndexAccess xDocumentIndexes = queryInterface(XIndexAccess.class, xDocumentIndexesSupplier.getDocumentIndexes());
        int indexcount = xDocumentIndexes.getCount();
        for (int i = 0; i < indexcount; i++) {
            // Update each index
            XDocumentIndex xDocIndex = queryInterface(XDocumentIndex.class, xDocumentIndexes.getByIndex(i));
            (queryInterface(XRefreshable.class, xDoc)).refresh();
            xDocIndex.update();
        }
        (queryInterface(XRefreshable.class, xDoc)).refresh();
    }

    private PropertyValue[] createProps(PropertyValue... pvs) {
        return pvs;
    }

    private PropertyValue p(String name, Object value) {
        PropertyValue pv = new PropertyValue();
        pv.Name = name;
        pv.Value = value;
        return pv;
    }

    public void close() {
        if (con != null) {
            try {
                con.disconnect();
            } catch (java.lang.Exception e) {
                e.printStackTrace();
            }
        }
    }
}