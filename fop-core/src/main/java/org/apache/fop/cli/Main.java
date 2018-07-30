/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id$ */

package org.apache.fop.cli;

import java.io.File;
import java.io.FileFilter;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Scanner;


import org.apache.commons.io.IOUtils;
import org.apache.commons.io.*;

import org.apache.fop.apps.*;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Main command-line class for Apache FOP.
 */
public final class Main {

    private Main() {
    }

    /**
     * @return the list of URLs to all libraries.
     * @throws MalformedURLException In case there is a problem converting java.io.File
     * instances to URLs.
     */
    public static URL[] getJARList() throws MalformedURLException {
        String fopHome = System.getProperty("fop.home");
        File baseDir;
        if (fopHome != null) {
            baseDir = new File(fopHome).getAbsoluteFile();
        } else {
            baseDir = new File(".").getAbsoluteFile().getParentFile();
        }
        File buildDir;
        if ("build".equals(baseDir.getName())) {
            buildDir = baseDir;
            baseDir = baseDir.getParentFile();
        } else {
            buildDir = new File(baseDir, "build");
        }
        File fopJar = new File(buildDir, "fop.jar");
        if (!fopJar.exists()) {
            fopJar = new File(baseDir, "fop.jar");
        }
        if (!fopJar.exists()) {
            throw new RuntimeException("fop.jar not found in directory: "
                    + baseDir.getAbsolutePath() + " (or below)");
        }
        List jars = new java.util.ArrayList();
        jars.add(fopJar.toURI().toURL());
        File[] files;
        FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".jar");
            }
        };
        File libDir = new File(baseDir, "lib");
        if (!libDir.exists()) {
            libDir = baseDir;
        }
        files = libDir.listFiles(filter);
        if (files != null) {
            for (File file : files) {
                jars.add(file.toURI().toURL());
            }
        }
        String optionalLib = System.getProperty("fop.optional.lib");
        if (optionalLib != null) {
            files = new File(optionalLib).listFiles(filter);
            if (files != null) {
                for (File file : files) {
                    jars.add(file.toURI().toURL());
                }
            }
        }
        URL[] urls = (URL[])jars.toArray(new URL[jars.size()]);
        /*
        for (int i = 0, c = urls.length; i < c; i++) {
            System.out.println(urls[i]);
        }*/
        return urls;
    }

    /**
     * @return true if FOP's dependecies are available in the current ClassLoader setup.
     */
    public static boolean checkDependencies() {
        try {
            //System.out.println(Thread.currentThread().getContextClassLoader());
            Class clazz = Class.forName("org.apache.commons.io.IOUtils");
            if (clazz != null) {
                clazz = Class.forName("org.apache.avalon.framework.configuration.Configuration");
            }
            return (clazz != null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Dynamically builds a ClassLoader and executes FOP.
     * @param args command-line arguments
     */
    public static void startFOPWithDynamicClasspath(String[] args) {
        try {
            final URL[] urls = getJARList();
            //System.out.println("CCL: "
            //    + Thread.currentThread().getContextClassLoader().toString());
            ClassLoader loader = (ClassLoader)
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        return new java.net.URLClassLoader(urls, null);
                    }
                });
            Thread.currentThread().setContextClassLoader(loader);
            Class clazz = Class.forName("org.apache.fop.cli.Main", true, loader);
            //System.out.println("CL: " + clazz.getClassLoader().toString());
            Method mainMethod = clazz.getMethod("startFOP", new Class[] {String[].class});
            mainMethod.invoke(null, new Object[] {args});
        } catch (Exception e) {
            System.err.println("Unable to start FOP:");
            e.printStackTrace();
            // @SuppressFBWarnings("DM_EXIT")
            System.exit(-1);
        }
    }

    /**
     * Executes FOP with the given arguments. If no argument is provided, returns its
     * version number as well as a short usage statement; if '-v' is provided, returns its
     * version number alone; if '-h' is provided, returns its short help message.
     *
     * @param args command-line arguments
     */
    public static void startFOP(String[] args) {
        //System.out.println("static CCL: "
        //    + Thread.currentThread().getContextClassLoader().toString());
        //System.out.println("static CL: " + Fop.class.getClassLoader().toString());
        CommandLineOptions options = null;
        FOUserAgent foUserAgent = null;
        OutputStream out = null;

        try {
            options = new CommandLineOptions();
            if (!options.parse(args)) {
                // @SuppressFBWarnings("DM_EXIT")
                System.exit(0);
            }

            foUserAgent = options.getFOUserAgent();
            String outputFormat = options.getOutputFormat();

            try {
                if (options.getOutputFile() != null) {
                    out = new java.io.BufferedOutputStream(
                            new java.io.FileOutputStream(options.getOutputFile()));
                    foUserAgent.setOutputFile(options.getOutputFile());
                } else if (options.isOutputToStdOut()) {
                    out = new java.io.BufferedOutputStream(System.out);
                }
                if (!MimeConstants.MIME_XSL_FO.equals(outputFormat)) {
                    options.getInputHandler().renderTo(foUserAgent, outputFormat, out);
                } else {
                    options.getInputHandler().transformTo(out);
                }
            } finally {
                IOUtils.closeQuietly(out);
            }

            // System.exit(0) called to close AWT/SVG-created threads, if any.
            // AWTRenderer closes with window shutdown, so exit() should not
            // be called here
            if (!MimeConstants.MIME_FOP_AWT_PREVIEW.equals(outputFormat)) {
                System.exit(0);
            }
        } catch (Exception e) {
            if (options != null) {
                options.getLogger().error("Exception", e);
                if (options.getOutputFile() != null) {
                    options.getOutputFile().delete();
                }
            }
            System.exit(1);
        }
    }


    public static int xml2pdf(String listaDir, String outPath, String lista) {
        URI baseURI = new File(".").getAbsoluteFile().toURI();
        int numberOfFiles = 0; 
        try {
            System.out.println("FOP ExampleXML2PDF\n");
            System.out.println("Preparing...");

            // Setup directories
            File baseDir = new File(listaDir);
            File outDir = new File(outPath, "out");
            outDir.mkdirs();

            // configure fopFactory as desired
            FopFactory fopFactory;
            FopFactoryBuilder fopFactoryBuilder;

            fopFactoryBuilder = new FopFactoryBuilder(baseURI);

            FileReader arq = new FileReader(lista);
            BufferedReader lerArq = new BufferedReader(arq);
            String xml;
            String config;
            String xsl; 
            int i = 0;

            config = lerArq.readLine();

            //o arquivo de configuração é setado neste trecho
            DefaultConfigurationBuilder cfgBuilder = new DefaultConfigurationBuilder();
            Configuration cfg = cfgBuilder.buildFromFile(new File(config));
            fopFactoryBuilder.setConfiguration(cfg);

            fopFactoryBuilder.setStrictFOValidation(true);
            fopFactoryBuilder.setTargetResolution(FopFactoryConfig.DEFAULT_TARGET_RESOLUTION);
            fopFactoryBuilder.setComplexScriptFeatures(false);
            fopFactory = fopFactoryBuilder.build();

            xsl = lerArq.readLine();

            System.out.println("Config file: " + config);
            System.out.println("xslt file  : " + xsl);

            do {
                xml = lerArq.readLine();
                if(xml == null){
                    break;
                }
                String ext1 = FilenameUtils.getExtension(xml);
                if(!ext1.equals("xml")){
                    //System.out.printf("%s não eh a extensão que queremos!",ext1);
                    break;
                }
                
                String s = "result"+i+".pdf";
                i++;
                // Setup input and output files
                File xmlfile = new File(baseDir, xml);
                File xsltfile = new File(baseDir, xsl);
                File pdffile = new File(outDir, s);
                //System.out.println("Input: XML (" + xmlfile.getName() + ")");
                //System.out.println("Stylesheet: " + xsltfile.getName());
                //System.out.println("Output: PDF (" + pdffile + ")");
                //System.out.println();
                //System.out.println("Transforming...");
                FOUserAgent foUserAgent = fopFactory.newFOUserAgent();

                // configure foUserAgent as desired
                // Setup output
                OutputStream out = new java.io.FileOutputStream(pdffile);
                out = new java.io.BufferedOutputStream(out);
                try {
                    // Construct fop with desired output format
                    Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);
                    // Setup XSLT
                    TransformerFactory factory = TransformerFactory.newInstance();
                    Transformer transformer = factory.newTransformer(new StreamSource(xsltfile));
                    // Set the value of a <param> in the stylesheet
                    transformer.setParameter("versionParam", "2.0");
                    // Setup input for XSLT transformation
                    Source src = new StreamSource(xmlfile);
                    // Resulting SAX events (the generated FO) must be piped through to FOP
                    Result res = new SAXResult(fop.getDefaultHandler());
                    // Start XSLT transformation and FOP processing
                    transformer.transform(src, res);

                } finally {
                    out.close();
                }
                System.out.println("Success!");
                numberOfFiles++;
            }while(true);
            arq.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(-1);
        }
        return numberOfFiles;
    }

    /**
     * The main routine for the command line interface
     * @param args the command line parameters
     */
    public static void main(String args[]) {
        Scanner ler = new Scanner(System.in);
        //String config = ler.nextLine("Qual o arquivo de configuração?");
        String listaDir = args[0];
        String outDir = args[1];
        String lista = args[2];
        //System.out.println("List dir: " + listaDir);
        //System.out.println("List    : " + lista);

        final long startTime = System.currentTimeMillis();

        int arqNum = xml2pdf(listaDir,outDir,lista);

        final long duration = System.currentTimeMillis() - startTime;
        float durationSec = (float) duration/1000;
        float eachSec = 0;
        if(arqNum != 0)
            eachSec = duration/arqNum;
        else{
            System.out.printf("Nenhum arquivo válido foi inserido como input!");
        }
            
        System.out.println("\nRendered " + arqNum + " files" );
        System.out.printf("Time (Total): %.3f s, (%d ms)\n",durationSec,duration );
        System.out.printf("Time (Each) : %.3f s, (%.0f ms)\n",eachSec/1000, eachSec);
        /*
        if (checkDependencies()) {
            startFOP(args);
        } else {
            startFOPWithDynamicClasspath(args);
        }*/
    }

}
