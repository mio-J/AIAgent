package com.ts.phi;

import android.util.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PromptTemplateManager {
    private static final String TAG = "PromptTemplateManager";
    private static final String TEMPLATE_FILE_PATH = "/data/local/tmp/prompt_templates.xml";
    
    private static PromptTemplateManager instance;
    private Map<String, String> templateCache;
    private boolean isLoaded = false;
    
    // Private constructor to implement the singleton pattern
    private PromptTemplateManager() {
        templateCache = new HashMap<>();
    }
    
    /**
    * Get the singleton instance
    */
    public static synchronized PromptTemplateManager getInstance() {
        if (instance == null) {
            instance = new PromptTemplateManager();
        }
        return instance;
    }

    /**
    * Initialize and load template files
    */
    public synchronized void initialize() {
        if (isLoaded) {
            Log.d(TAG, "Templates already loaded");
            return;
        }
        loadTemplatesFromFile();
    }

    /**
    * Process escape characters in strings
    */
    private String processEscapeCharacters(String rawString) {
        if (rawString == null) {
            return "";
        }
    
        String processed = rawString;
    
        // Process common escape characters
        processed = processed.replace("\\n", "\n");        // newline character
        processed = processed.replace("\\t", "\t");        // tab character
        processed = processed.replace("\\r", "\r");        // carriage return
        processed = processed.replace("\\'", "'");         // single quote
        processed = processed.replace("\\\"", "\"");       // double quote
        processed = processed.replace("\\\\", "\\");       // backslash (last)
        return processed;
    }
    
    /**
    * Load templates from external XML file
    */
    private void loadTemplatesFromFile() {
        try {
            File xmlFile = new File(TEMPLATE_FILE_PATH);
            if (!xmlFile.exists()) {
                Log.e(TAG, "Template file not found: " + TEMPLATE_FILE_PATH);
                return;
            }
            
            if (!xmlFile.canRead()) {
                Log.e(TAG, "Cannot read template file: " + TEMPLATE_FILE_PATH);
                return;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);

            // Get all string elements
            NodeList stringNodes = doc.getElementsByTagName("string");
            templateCache.clear(); 

            for (int i = 0; i < stringNodes.getLength(); i++) {
                Element element = (Element) stringNodes.item(i);
                String name = element.getAttribute("name");
                String value = element.getTextContent();
                
                if (name != null && !name.isEmpty()) {
                    // Process escape characters
                    String processedValue = processEscapeCharacters(value);
                    templateCache.put(name, processedValue);
                    Log.d(TAG, "Loaded template: " + name);
                }
            }
            isLoaded = true;
            Log.i(TAG, "Successfully loaded " + templateCache.size() + " templates from external file");
        } catch (Exception e) {
            Log.e(TAG, "Error loading templates from file: " + e.getMessage(), e);
            isLoaded = false;
        }
    }
    
    /**
     * Get the raw template content
     */
    public String getTemplate(String templateName) {
        if (!isLoaded) {
            initialize();
        }
        
        String template = templateCache.get(templateName);
        if (template == null) {
            Log.w(TAG, "Template not found: " + templateName);
            return "";
        }
        
        return template;
    }
    
    /**
    * Get the formatted template content
    */
    public String getFormattedTemplate(String templateName, Object... args) {
        String template = getTemplate(templateName);
        if (template.isEmpty()) {
            return getTemplate("intentionPrompt_one");
        }
        // if (template.isEmpty()) {
        //     return "";
        // }
        try {
            return String.format(template, args);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting template " + templateName + ": " + e.getMessage());
            return template; // Return the unformatted template
        }
    }
    
    /**
     * Check if a template named xxx exists.（eg:promptTemplate_normal_agree）
     */
    public boolean hasTemplate(String templateName) {
        if (!isLoaded) {
            initialize();
        }
        return templateCache.containsKey(templateName);
    }
    
    /**
    * Get the count of loaded templates
    */
    public int getLoadedTemplateCount() {
        if (!isLoaded) {
            initialize();
        }
        return templateCache.size();
    }
    
    /**
    * Check if the template file exists
    */
    public boolean isTemplateFileExists() {
        File file = new File(TEMPLATE_FILE_PATH);
        return file.exists() && file.canRead();
    }
}