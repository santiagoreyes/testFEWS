/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package enviarfe;

import com.opencsv.CSVReader;
import enviarfe.wscliente.*;
import enviarfe.wscliente.MensajeDocumentoElectronico;
import enviarfe.wscliente.RespuestaMensaje;
import java.io.BufferedReader;
import org.w3c.dom.Document;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.transform.Transformer;
import org.w3c.dom.Node;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;

import javax.xml.parsers.*;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;

//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author santi
 */
public class EnviarFE {

    /**
     * @param args the command line arguments
     */
    private static String pathArchivos = "src/archivos/";
    private static String constanteNombreArchivoEjemplo = pathArchivos+"ejemploFE.xml";
    private static String constanteNombreArchivoConfiguracionCampos = pathArchivos+"camposConfiguracion.json";
    private static String constanteNombreArchivoDatos = pathArchivos+"datos.csv";
    private static String constanteNombreArchivoResultado = pathArchivos+"resultados.xml";
    private static String constanteId = "id";
    private static String constanteDescripcion = "descripcion";
    private static String constanteEsperado = "esperado";

    public static void main(String[] args) {
        //testSolicitarAutorizacionDE();
        ejecutarPrueba();
        //testGrabarXml();
    }


    private static void ejecutarPrueba() {
        Document documento = leerDocumentoXml(constanteNombreArchivoEjemplo);
        HashMap camposConfig = getCamposArchivoJson(constanteNombreArchivoConfiguracionCampos);
        //System.out.println("xPath " + campos.get("codigoUnicoDocumento"));
        List<HashMap> listaDatos = readCsv(constanteNombreArchivoDatos);
        List<RespuestaMensaje> respuestas = new ArrayList<RespuestaMensaje>();
        Escenarios escenarios = new Escenarios();
        System.out.println("datos " +listaDatos.size());
        for (int i = 0; i < listaDatos.size(); i++) {
            //System.out.println("Antes " + getValorFromXml(documento, "//codigoUnicoDocumento"));
            //System.out.println("Fila" + i); 
            HashMap camposFila=listaDatos.get(i);
            String id = camposFila.get(constanteId).toString();
            String descripcion = camposFila.get(constanteDescripcion).toString();
            String esperado = camposFila.get(constanteEsperado).toString();

            Escenario escenario = new Escenario(id,descripcion, esperado,camposFila);
            documento = reemplazarCamposVariables(documento, camposConfig, camposFila);
            MensajeDocumentoElectronico mensaje = crearMensaje(documento);
            RespuestaMensaje respuesta
                    = solicitarAutorizacionDE(mensaje);
            escenario.setRespuestaMensaje(respuesta);
            //respuestas.add(respuesta);
            //System.out.println("Despues " + getValorFromXml(documento, "//codigoUnicoDocumento"));
            escenarios.add(escenario);
        }
        //
        grabarResultado(escenarios);
        
        for (int i = 0; i < respuestas.size(); i++) {
            System.out.println("Respuesta "
                    + respuestas.get(i).getRespuesta().getValue().getResultadoProceso().getValue().getResultadoProceso());
        }
    }

        private static void grabarResultado(Escenarios escenarios) {
        try {
            Serializer serializer = new Persister();
            File result = new File(constanteNombreArchivoResultado);
            serializer.write(escenarios, result);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    
    private static RespuestaMensaje solicitarAutorizacionDE(enviarfe.wscliente.MensajeDocumentoElectronico mensajeDE) {
        enviarfe.wscliente.FEServicio service = new enviarfe.wscliente.FEServicio();
        enviarfe.wscliente.IFEServicios port = service.getBasicHttpBindingIFEServicios();
        return port.solicitarAutorizacionDE(mensajeDE);
    }

    private static List<HashMap> readCsv(String pathNombreCsv) {
        List<HashMap> datosLinea = new ArrayList<HashMap>();
        try {
            CSVReader reader = new CSVReader(new FileReader(pathNombreCsv));
            List<String[]> listaDatos = reader.readAll();
            for (int i = 0; i < listaDatos.size(); i++) {
                String[] fila = listaDatos.get(i);
                HashMap lineaCampoValor = new HashMap();
                for (int j = 0; j < fila.length; j++) {
                    lineaCampoValor.put(listaDatos.get(0)[j], fila[j]);
                }
                if (i > 0) {
                    datosLinea.add(lineaCampoValor);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return datosLinea;
    }

    private static void testSolicitarAutorizacionDE() {
        try {
            RespuestaMensaje respuesta = solicitarAutorizacionDE(crearMensaje());

            String respuestaEstado = respuesta.getRespuesta().getValue().getMensajeRespuesta().getValue().toString();
            System.out.println("Respuesta " + respuestaEstado);
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
        }

    }

    private static enviarfe.wscliente.MensajeDocumentoElectronico
            crearMensaje(Document documento) {
        ObjectFactory factory = new ObjectFactory();
        MensajeDocumentoElectronico mensaje = factory.createMensajeDocumentoElectronico();
        //Document documento = leerDocumentoXml(constanteNombreArchivoEjemplo);
        String cufe = getValorFromXml(documento, "//codigoUnicoDocumento");
        mensaje.setCufe(factory.createMensajeDocumentoElectronicoCufe(cufe));
        String documentoXml = getStringFromDocument(documento);
        mensaje.setDocumentoElectronico(factory.createMensajeDocumentoElectronicoDocumentoElectronico(documentoXml));
        return mensaje;
    }

    private static enviarfe.wscliente.MensajeDocumentoElectronico crearMensaje() throws SAXException, IOException, XPathExpressionException, ParserConfigurationException {
        ObjectFactory factory = new ObjectFactory();

        MensajeDocumentoElectronico mensaje = factory.createMensajeDocumentoElectronico();
        Document documento = leerDocumentoXml(constanteNombreArchivoEjemplo);
        String cufe = getValorFromXml(documento, "//codigoUnicoDocumento");
        mensaje.setCufe(factory.createMensajeDocumentoElectronicoCufe(cufe));
        String documentoXml = getStringFromDocument(documento);
        mensaje.setDocumentoElectronico(factory.createMensajeDocumentoElectronicoDocumentoElectronico(documentoXml));
        return mensaje;
    }

    private static String getValorFromXml(Document documento, String pxpath) {
        String valor = "";
        try {
            XPathFactory xpathFactory = XPathFactory.newInstance();
// Create XPath object
            XPath xpath = xpathFactory.newXPath();
            XPathExpression expr = xpath.compile(pxpath);
            valor = (String) expr.evaluate(documento, XPathConstants.STRING);
            //System.out.println("Valor: " + valor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return valor;
    }

    private static String getStringFromDocument(Document doc) {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch (TransformerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static Document leerDocumentoXml(String pathNombreArchivo) {
        // Create a document by parsing a XML file
        Document document = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            File file = new File(pathNombreArchivo);
            document = builder.parse(file);
            //document = document.getDocumentElement().normalize();
            //System.out.println("Archivo Leido");

        } catch (Exception ex) {
            //throws SAXException, IOException, ParserConfigurationException
            ex.printStackTrace();
        }
        return document;

    }

    private static Document reemplazarCampo(Document document, String xPath, String valor) {
        try {
            // Get a node using XPath
            XPath xPathObject = XPathFactory.newInstance().newXPath();
            String expression = xPath;
            Node node = (Node) xPathObject.evaluate(expression, document, XPathConstants.NODE);
            System.out.println("REEMPLAZANDO xpath " + xPath + " valor " + valor);
            // Set the node content
            node.setTextContent(valor);

            // Write changes to a file
            //Transformer transformer = TransformerFactory.newInstance().newTransformer();
            //transformer.transform(new DOMSource(document), new StreamResult(new File("C:/temp/test-updated.xml")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;

    }

    private static Document reemplazarCamposVariables(Document document,
            HashMap camposConfig, HashMap datos) {

        Iterator it = datos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            String path = "";
            String valorActual, valorDespues, valorDato = "";
            if (camposConfig.get(pair.getKey()) != null) {
                path = camposConfig.get(pair.getKey()).toString();
                valorActual = getValorFromXml(document, path);
                valorDato = pair.getValue().toString();
                document = reemplazarCampo(document, path, valorDato);
                valorDespues = getValorFromXml(document, path);
                System.out.println(
                        pair.getKey() + " = " + valorDato
                        + " xpath " + path
                        + " valor " + valorActual
                        + " valor despues" + valorDespues
                //+ getValorFromXml(document,path)
                );
            }

            //it2.remove(); // avoids a ConcurrentModificationException
        }
        return document;

    }

    private static HashMap getCamposArchivoJson(String pathNombreArchivo) {

        HashMap campos = new HashMap();
        JSONParser parser = new JSONParser();

        try {

            Object obj = parser.parse(new FileReader(pathNombreArchivo));
            JSONObject jsonObject = (JSONObject) obj;

            org.json.simple.JSONArray jsonArreglo
                    = (JSONArray) jsonObject.get("conf");

            for (int i = 0; i < jsonArreglo.size(); i++) {
                JSONObject campo = (JSONObject) jsonArreglo.get(i);
                campos.put(campo.get("campo"), campo.get("xpath"));
                //System.out.println(campos.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return campos;
    }

}
