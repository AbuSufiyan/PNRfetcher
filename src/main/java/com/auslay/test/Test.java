package com.auslay.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Test {

	public static void main(String[] args) throws Exception {

		String body = null;
		String pnr;
		StringBuilder passengerDetails = new StringBuilder();
		if (args.length > 0) {

			args = checkIfFileSuppliedForPNR(args);
			for (int count = 0; count < args.length; count++) {
				
				pnr = args[count];

				body = FetchCurrentStatus(pnr);
				passengerDetails.append(FetchDetails(body, pnr,count==0));

			}
			// System.out.println(body);

		} else {
			passengerDetails
					.append("<h2>PNR is empty.</h2></br> sample: java -jar Test 4810991753");
			System.out
					.println("PNR missing. sample: java -jar Test 4810991753.");
		}

		File new03 = new File("Status.html");
		FileWriter fileWriter = new FileWriter(new03);
		fileWriter.write(passengerDetails.toString().replaceAll("\n", "</br>"));
		fileWriter.flush();
		fileWriter.close();

		ProcessBuilder pBuilder = new ProcessBuilder("cmd.exe", "/c", "start "
				+ "Status.html");
		pBuilder.redirectErrorStream(true);
		Process startDirectory = pBuilder.start();

	}

	private static String[] checkIfFileSuppliedForPNR(String[] args)
			throws IOException {
		if (args.length == 1) {
			if (args[0].contains("txt")) {
				return parseTextForPnr(args[0]);
			}
		}
		return args;

	}

	private static String[] parseTextForPnr(String fileName) throws IOException {
		ArrayList pnrList = new ArrayList();
		FileReader pnrReader = new FileReader(fileName);
		BufferedReader reader = new BufferedReader(pnrReader);
		String pnr = reader.readLine();

		while (pnr != null) {
			pnrList.add(pnr);
			pnr = reader.readLine();
		}
		reader.close();
		//String[] args = new String[pnrList.size()];
		return (String[]) pnrList.toArray(new String[pnrList.size()]);

	}

	private static StringBuilder FetchDetails(String body, String PNR, boolean firstEntry)
			throws Exception {
		
		StringBuilder passengerDetails = new StringBuilder();
		// Empty response
		if (body == null) {
			passengerDetails
					.append("<h2>Could not receive response.Check INternet Connection.</h2></br> sample: java -jar Test 4810991753");
			return passengerDetails;

		}// PNR was invalid
		else if (body.contains("Following ERROR was encountered")) {
			passengerDetails
					.append("</br></br><b style=\"color:red\">Incorrect PNR : "
							+ PNR + "</b>");
			System.out.println("Incorrect PNR");
			return passengerDetails;
		}// Valid Data Found
		else {
			Document doc = Jsoup.parse(body);
			//Parse passenger details
			Element element = doc.getElementById("center_table");
			//Parse train details
			Elements trainElements = doc.getElementsByClass("table_border_both");
//			doc.get
			String trainDetails = getTrainDetails(trainElements);
						
			String data = element.text();
			return DisplayDetails(data, PNR, firstEntry,trainDetails);

		}

	}

	private static String getTrainDetails(Elements trainElements)
	{
		StringBuilder trainDetails = new StringBuilder();
		int i=0;
		for (Element element : trainElements) {
			
			switch (i) {
			case 0:trainDetails.append("" +element.text());i++;break;
			case 1:trainDetails.append(" - "+element.text());i++;break;
			case 2:trainDetails.append(" \nJourney : <b>"+element.text()+"</b>");i++;break;
			case 3:trainDetails.append(" , "+element.text());i++;break;
			case 4:trainDetails.append(" ->  "+element.text());i++;break;
			case 7:trainDetails.append("\nClass \t\t: "+element.text()+"\n\n");i++;break;
			default:
				i++;break;
			}
		}
		return trainDetails.toString();
	}
	
	
	private static String FetchCurrentStatus(String pnr) throws IOException {
		URL url = new URL(
				"http://www.indianrail.gov.in/cgi_bin/inet_pnrstat_cgi.cgi");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setDoOutput(true);

		String urlParams = "lccp_cap_val=123&lccp_capinp_val=123&lccp_pnrno1=" + pnr + "&submit=Get+Status";
		// 4810991753
		DataOutputStream dOutStream = new DataOutputStream(
				con.getOutputStream());
		dOutStream.writeBytes(urlParams);
		dOutStream.flush();
		dOutStream.close();

		InputStream in = con.getInputStream();
		String encoding = con.getContentEncoding();
		encoding = encoding == null ? "UTF-8" : encoding;
		return IOUtils.toString(in, encoding);
	}

	private static StringBuilder DisplayDetails(String data, String PNR,
			boolean firstEntry, String trainDetails) throws Exception {
		String result = data.substring(data.indexOf("Passenger"));
		String passenegerStatusString = result.substring(0,
				result.indexOf("Charting Status"));
		StringBuilder sb = new StringBuilder();

		Pattern p = Pattern.compile("Passenger");
		Matcher m = p.matcher(result);
		int index = passenegerStatusString.indexOf("Passenger");
		int startindex = 0;
		if (!firstEntry) {
			sb.append("\n\n----------------------------------\n\n");
		}
		
		sb.append(trainDetails);
		sb.append("PNR: " + PNR);
		while (index >= 0) {
			sb.append(passenegerStatusString.substring(startindex, index));
			sb.append("\n");
			startindex = index;
			index = passenegerStatusString.indexOf("Passenger", index + 1);
			if (index == -1) {
				sb.append(passenegerStatusString.substring(startindex));
			}

		}
		System.out.println(sb);
		return sb;
	}

}
