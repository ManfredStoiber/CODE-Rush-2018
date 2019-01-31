package com.stoibersoftware;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.javafx.collections.MappingChange.Map;

/**
 * Servlet implementation class WebHookServlet
 */
@WebServlet("/WebHookServlet")
public class WebHookServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public WebHookServlet() {
    	
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		
		String requestJSONString = "";
		BufferedReader requestReader = request.getReader();
		for(String line = requestReader.readLine(); line != null; line = requestReader.readLine()) {
			requestJSONString += line;
		}
		
		

		JsonObject requestJSON = new JsonParser().parse(requestJSONString).getAsJsonObject();
		String queryProduct = requestJSON.getAsJsonObject("queryResult").getAsJsonObject("parameters").get("Product").getAsString();
		String queryColor = requestJSON.getAsJsonObject("queryResult").getAsJsonObject("parameters").get("Color").getAsString();
		

		String filter = getFilterFromColor(queryColor);
		boolean colorInvalid = false;
		
		URL baurUrl;
		if(filter.equals("")) {
			// ohne Color
			baurUrl = new URL("https://www.baur.de/suche/serp/magellan?query=" + queryProduct + "&start=0&locale=de_DE&count=24&clientId=BaurDe");
			colorInvalid = true;
		} else {
			// mit Color
			baurUrl = new URL("https://www.baur.de/suche/serp/magellan?query=" + queryProduct + "&filterValues=filter_color=" + getFilterFromColor(queryColor) + "&start=0&locale=de_DE&count=24&clientId=BaurDe");
		}
		URLConnection con = baurUrl.openConnection();
		String responseString = "";
		BufferedReader responseReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
		for(String line = responseReader.readLine(); line != null; line = responseReader.readLine()) {
			responseString += line;
		}
		JsonObject resultJSON = new JsonParser().parse(responseString).getAsJsonObject();
		
		try {
			String productName = resultJSON.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().get("name").getAsString();
			String productPrice = resultJSON.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().getAsJsonObject("price").get("value").getAsString();
			String sku = resultJSON.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().get("sku").getAsString().split("-")[0];
			String imageUri = resultJSON.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().getAsJsonArray("images").get(0).getAsString();
		
			JsonObject responseJSON = new JsonObject();
			if(colorInvalid) {
				responseJSON.addProperty("fulfillmentText", "Ich habe für dich folgendes Produkt gefunden: " + productName + " für " + productPrice + " Euro, aber leider nicht in der gewünschten Farbe");
			} else {
				responseJSON.addProperty("fulfillmentText", "Ich habe für dich folgendes Produkt gefunden: " + productName + " für " + productPrice + " Euro");	
			}
//			responseJSON.add("fulfillmentMessages", new JsonParser().parse("[]"));
			responseJSON.addProperty("source", "example.com");
			responseJSON.add("payload", new JsonParser().parse("{\r\n" + 
					"        \"google\": {\r\n" + 
					"            \"expectUserResponse\": true,\r\n" + 
					"            \"richResponse\": {\r\n" + 
					"                \"items\": [\r\n" + 
					"                    {\r\n" + 
					"                        \"simpleResponse\": {\r\n" + 
					"                            \"textToSpeech\": \"Ich habe für dich " + productName + " für " + productPrice + " Euro gefunden\"\r\n" + 
					"                        }\r\n" + 
					"                    },\r\n" + 
					"                    {\r\n" + 
					"                        \"basicCard\": {\r\n" + 
					"                            \"title\": \"" + productName + "\",\r\n" + 
					"                            \"subtitle\": \"" + productPrice + " EUR\",\r\n" + 
					"                            \"image\": {\r\n" + 
					"                                \"url\": \"https://media.baur.de/i/empiriecom/" + imageUri + "\",\r\n" + 
					"                                \"accessibilityText\": \"Produktbild\"\r\n" + 
					"                            },\r\n" + 
					"                            \"buttons\": [\r\n" + 
					"                                {\r\n" + 
					"                                    \"title\": \"Im Shop öffnen\",\r\n" + 
					"                                    \"openUrlAction\": {\r\n" + 
					"                                        \"url\": \"https://baur.de/s/" + sku + "\"\r\n" + 
					"                                    }\r\n" + 
					"                                }\r\n" + 
					"                            ],\r\n" + 
					"                            \"imageDisplayOptions\": \"WHITE\"\r\n" + 
					"                        }\r\n" + 
					"                    }\r\n" + 
					"                ]\r\n" + 
					"            }\r\n" + 
					"        }\r\n" + 
					"    }"));
			
			responseJSON.add("fulfillmentMessages", new JsonParser().parse("[\r\n" + 
					"    {\r\n" + 
					"      \"card\": {\r\n" + 
					"        \"title\": \"" + productName + "\",\r\n" + 
					"        \"subtitle\": \"" + productPrice + "EUR\",\r\n" + 
					"        \"imageUri\": \"https://media.baur.de/i/empiriecom/" + imageUri + "\",\r\n" + 
					"        \"buttons\": [\r\n" + 
					"          {\r\n" + 
					"            \"text\": \"Artikel öffnen\",\r\n" + 
					"            \"postback\": \"https://baur.de/s/" + sku + "/\"\r\n" + 
					"          }\r\n" + 
					"        ]\r\n" + 
					"      }\r\n" + 
					"    }\r\n" + 
					"  ]"));
			
			out.print(responseJSON.toString());
		} catch (Exception e) {
			// no product found, try without filter
			if(!colorInvalid) {
				baurUrl = new URL("https://www.baur.de/suche/serp/magellan?query=" + queryProduct + "&start=0&locale=de_DE&count=24&clientId=BaurDe");
			
				URLConnection con2 = baurUrl.openConnection();
				String responseString2 = "";
				BufferedReader responseReader2 = new BufferedReader(new InputStreamReader(con2.getInputStream()));
				for(String line = responseReader2.readLine(); line != null; line = responseReader2.readLine()) {
					responseString2 += line;
				}
				JsonObject resultJSON2 = new JsonParser().parse(responseString2).getAsJsonObject();
				try {
					String productName = resultJSON2.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().get("name").getAsString();
					String productPrice = resultJSON2.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().getAsJsonObject("price").get("value").getAsString();
					String sku = resultJSON2.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().get("sku").getAsString().split("-")[0];
					String imageUri = resultJSON2.getAsJsonObject("searchresult").getAsJsonObject("result").getAsJsonArray("styles").get(0).getAsJsonObject().getAsJsonArray("images").get(0).getAsString();
					
				
					JsonObject responseJSON = new JsonObject();

					responseJSON.addProperty("fulfillmentText", "Ich habe für dich folgendes Produkt gefunden: " + productName + " für " + productPrice + " Euro, aber leider nicht in der gewünschten Farbe");	
					responseJSON.add("fulfillmentMessages", new JsonParser().parse("[\r\n" + 
							"    {\r\n" + 
							"      \"card\": {\r\n" + 
							"        \"title\": \"" + productName + "\",\r\n" + 
							"        \"subtitle\": \"" + productPrice + "EUR\",\r\n" + 
							"        \"imageUri\": \"https://media.baur.de/i/empiriecom/" + imageUri + "\",\r\n" + 
							"        \"buttons\": [\r\n" + 
							"          {\r\n" + 
							"            \"text\": \"Artikel öffnen\",\r\n" + 
							"            \"postback\": \"https://baur.de/s/" + sku + "/\"\r\n" + 
							"          }\r\n" + 
							"        ]\r\n" + 
							"      }\r\n" + 
							"    }\r\n" + 
							"  ]"));
					
					out.print(responseJSON.toString());
				} catch (Exception e2) {
					JsonObject responseJSON = new JsonObject();
					responseJSON.addProperty("fulfillmentText", "Ich habe leider kein passendes Produkt gefunden");
				}
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public String getFilterFromColor(String color) {
    	try {
			
			BufferedReader br = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream("/WEB-INF/filter.csv")));
			for(String line = br.readLine(); line != null; line = br.readLine()) {
				String[] values = line.split(";");
				if(values[0] != null && values[0].equals("filter_color") && values[2] != null && values[3] != null && values[2].equals(color))  {
					return values[3];
				}
			}
		} catch (IOException e ) {
			e.printStackTrace();
		}
    	
    	return "";
	}
	
	public String getColorFromColor(String color) {
		try {
			
			BufferedReader br = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream("/WEB-INF/color.csv")));
			for(String line = br.readLine(); line != null; line = br.readLine()) {
				String[] values = line.split(";");
				if(values[0] != null && values[0].equals(color))  {
					return values[1];
				}
			}
		} catch (IOException e ) {
			e.printStackTrace();
		}
		return "";
	}

}
