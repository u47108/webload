/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mobext.dev.process;

import cl.clarochile.companyusermanagement.AddUserRequest;
import cl.clarochile.companyusermanagement.AddUserResponse;
import cl.clarochile.companyusermanagement.CompanyUserManagement;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.*;
import cl.clarochile.companyusermanagement.CompanyUserManagement_Service;
import cl.clarochile.webservices.schemas.CompanyUserType;
import java.util.Date;
import javax.xml.ws.BindingProvider;
import cl.fusiona.utils.GenericValidation;
import java.net.MalformedURLException;
import javax.xml.namespace.QName;
import java.net.URL;

/**
 *
 * @author luis
 */
public class Main {

    static final Logger log = Logger.getLogger(Main.class.getName());
    static Config conf;
    private static final int BUFFER = 1024;
    static private final int FILE_EMPRESA = 0;
    private static final Integer WS_TIMEOUT = 10000;
    private static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";
    private static final String CONNECT_TIMEOUT = "com.sun.xml.internal.ws.request.timeout";
    private static final String CONNECT_TIMEOUT_CLIENT = "com.sun.xml.internal.ws.connect.timeout";
    private static String urlWSEmpresas;
    private static String extensionArchivos;
    GenericValidation validate = new GenericValidation();
    List<String> invalidFile = new ArrayList<String>();
    CompanyUserManagement_Service stub;
    CompanyUserManagement companyUserManagement;
    private static boolean deleteFiles;
    private static String rutaArchivos;

    FilenameFilter fileNameFilter = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {

            if (name.lastIndexOf('.') > 0) {

                // get last index for '.' char
                int lastIndex = name.lastIndexOf('.');

                // get extension
                String str = name.substring(lastIndex);

                // match path name extension
                if (str.equals(".txt") || str.equals(".csv")) {
                    return true;
                }
            }

            return false;
        }
    };

    public Main() throws MalformedURLException {
        //URL wsdlLocation = new URL(urlWSEmpresas);
        //QName serviceName=new QName("http://www.clarochile.cl/CompanyUserManagement/");
        try {
            stub = new CompanyUserManagement_Service();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Errror al instanciar....");
        }
        //stub = new CompanyUserManagement_Service(wsdlLocation, serviceName);
        companyUserManagement = stub.getCompanyUserManagementSOAP();
    }

    public static void main(String args[]) throws Exception {
        conf = new Config();
        deleteFiles = conf.getProperty("dir.deleteFiles", "false").equalsIgnoreCase("true");
        rutaArchivos = conf.getProperty("dir.input", "/home/luis/HAVAS/CargaInicialEmpresas");
        urlWSEmpresas = conf.getProperty("url.company", "http://wlproducersucvirtest.clarochile.org/CompanyUserManagementWS/CompanyUserManagement");
        extensionArchivos = conf.getProperty("dir.extension", "csv");
        setLogger();
        System.err.println("Loading  Carga Inicial Empresas . . . ");

        Main dts = new Main();
        dts.execute();
    }

    public boolean execute() {
        log.info("---- Iniciando proceso ---- ");

        List<String> files = buscarArchivos(rutaArchivos, extensionArchivos);
        if (files == null) {
            log.severe("Error buscando archivos. Proceso finalizado");
            return false;
        } else if (files.size() == 0) {
            log.info("No hay archivos por procesar. Proceso finalizado");
            return true;
        }
        //se procesa el primero
        if (cargarBaseClientes(files.get(0))) {
            return true;
        } else {
            return false;
        }
    }

    private void deleteArchivo(String nomArchivo) {
        //borrar archivo
        File fichero = new File(nomArchivo);
        if (fichero.exists()) {
            fichero.delete();
        }
    }

    public boolean cargarBaseClientes(String file) {
        SimpleDateFormat sdfSalida = new SimpleDateFormat("yyyymmdd");
        String sFechaHoy = sdfSalida.format(new Date());
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            reader.readLine(); //skip first line
            String line;
            int count = 0;
            int linea = 0;
            while ((line = readLine(reader)) != null) {
                String datos[] = line.split(";");
                String rutEmpresa = datos[0];
                String nombreAdmin = datos[1];
                String rutAdmin = datos[2];
                String emailAdmin = datos[3];
                boolean valid = false;
                linea++;

                if (validate.esRutValido(rutEmpresa) && validate.esRutValido(rutAdmin) && validate.isEmail(emailAdmin) && validate.required(nombreAdmin)) {
                    valid = true;
                    //CREAR OBJETO Y ENVIAR WS
                    AddUserRequest user = new AddUserRequest();
                    CompanyUserType comp = new CompanyUserType();
                    //comp.setClave("claro2017");
                    comp.setCorreo(emailAdmin);
                    comp.setRutEmpresa(rutEmpresa.toUpperCase());
                    comp.setRutUsuario(rutAdmin.toUpperCase());
                    comp.setRol("ADMINISTRADOR");
                    comp.setAlias("Admin");
                    //comp.setAccessType("FUJJO");
                    comp.setAccessTrafico(1);
                    comp.setAccessFactura(1);
                    comp.setAccessTicket(1);
                    comp.setAccessReporte(1);
                    
                    user.setCompanyUser(comp);
                    if (notificarWS(user)) {
                        log.severe("Proceso registro N[" + linea + "]");
                    } else {
                        //error en linea
                        log.severe("Error en line [" + linea + "]: " + line + ".\n");
                    }

                } else {
                    log.severe("Error importing line [" + linea + "]: " + line + ".\n");
                    invalidFile.add(file);
                }

                //envio de correo 
            }
            reader.close();

        } catch (Exception error) {

        }
        return true;
    }

    private String readLine(BufferedReader reader) throws IOException {
        String s = null;
        while ((s = reader.readLine()) != null) {
            s = s.trim();
            if (!s.equals("") && s.indexOf("rows selected") == -1 && !s.startsWith("----")) {
                break;
            }
        }
        return s;
    }

    private String getFileName(String company, int type) {
        final DateFormat df = new SimpleDateFormat("yyyyMMdd");
        Calendar c = Calendar.getInstance();
        String path = conf.getProperty("dir.output");
        String filePath = path + File.separator;
        filePath += "BaseTest.CargaCredAcceso";
        filePath += "_PROCESADO_" + df.format(c.getTime()) + ".txt";
        return filePath;
    }

    private static void setLogger() {
        String logFile = conf.getProperty("log.file", "logs/log_proceso.log");
        boolean console = conf.getProperty("log.console", "false").equalsIgnoreCase("true");
        try {
            FileHandler fh = new FileHandler(logFile, true);
            log.addHandler(fh);
            if (console) {
                log.addHandler(new ConsoleHandler());
            }
            log.setUseParentHandlers(false);
            Handler h[] = log.getHandlers();
            LogFormatter lf = new LogFormatter();
            for (int i = 0; i < h.length; i++) {
                h[i].setFormatter(lf);
            }
        } catch (Exception e) {
            System.err.println("No se puede abrir archivo de log " + logFile + ". " + e);
        }
    }

    private List<String> buscarArchivos(String rutaArchivos, String ext) {
        List<String> archivos = new ArrayList<String>();
        File file = new File(rutaArchivos);
        if (!file.exists()) {
            System.out.println(rutaArchivos + " Directory doesn't exists");
        }
        File[] listFiles = file.listFiles(fileNameFilter);
        if (listFiles.length == 0) {
            System.out.println(rutaArchivos + "doesn't have any file with extension " + ext);
        } else {
            for (File f : listFiles) {
                System.out.println("File: " + rutaArchivos + File.separator + f.getName());
                archivos.add(f.getName());
            }
        }
        return archivos;
    }

    private static class LogFormatter extends Formatter {

        static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        public String format(LogRecord record) {
            StringBuffer builder = new StringBuffer(1000);
            builder.append(df.format(new java.util.Date(record.getMillis()))).append(" - ");
            builder.append("[").append(record.getSourceClassName()).append(".");
            builder.append(record.getSourceMethodName()).append("] - ");
            builder.append("[").append(record.getLevel()).append("] - ");
            builder.append(formatMessage(record));
            builder.append("\n");
            return builder.toString();
        }
    }

    public boolean notificarWS(AddUserRequest add) {
        log.severe("entorno.cargando propiedades");

        String ENDPOINT = urlWSEmpresas;

        log.severe("*** iniciando enviarEmail ***");
        log.severe("url [" + ENDPOINT + "]");
        log.severe("correo [" + add.getCompanyUser().getCorreo() + "]");
        log.severe("mensaje [" + add.getCompanyUser().toString() + "]");

        try {

            ((BindingProvider) companyUserManagement).getRequestContext().put(REQUEST_TIMEOUT, WS_TIMEOUT);
            ((BindingProvider) companyUserManagement).getRequestContext().put(CONNECT_TIMEOUT, WS_TIMEOUT);
            ((BindingProvider) companyUserManagement).getRequestContext().put(CONNECT_TIMEOUT_CLIENT, WS_TIMEOUT);
            ((BindingProvider) companyUserManagement).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT);

            AddUserResponse response = companyUserManagement.addUser(add);

            if (response != null && response.getStatus().getStatus() == 0) {

                log.severe("Se encola el mensaje: " + response.getStatus().getStatus());

            } else {
                log.severe("Error : " + response.getStatus().getMessage());

                return false;
            }
        } catch (Exception e) {
            log.severe("Error al invocar servicio" + e);
            return false;
        }
        return true;
    }

}
