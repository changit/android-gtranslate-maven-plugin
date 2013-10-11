package kws.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

@Mojo(name = "translate")
public class GTranslator extends AbstractMojo {
	@Parameter
	protected String googleKey;

	@Parameter
	protected File originalLanguageFile;

	@Parameter
	protected String originalLanguage;

	@Parameter
	protected String[] languages;

	@Parameter
	protected File destDirectory;

	private static String googleUrl = "https://www.googleapis.com/language/translate/v2";

	public void execute() throws MojoExecutionException {
		getLog().info("Executing Android 118n auto tranlation ....");
		getLog().info("Original Language File " + originalLanguageFile);
		getLog().info("Original language  " + originalLanguage);
		getLog().info("Output Languages " + Arrays.asList(languages));
		getLog().info("Destination Directory " + destDirectory);

		try {
			Map<String, String> originalLanguageEntries = readEntries();
			if(languages != null){
				for(int i = 0; i < languages.length; i++){
					Map<String, String> tranlated = translate(originalLanguageEntries, languages[i]);
					saveAsAFile(tranlated,  languages[i]);
				}
			}

		} catch (Exception e) {
			getLog().error(e);
		}
	}

	protected void saveAsAFile(Map<String, String> tranlated, String langPrefix) throws IOException {
		File valueFolder = new File(destDirectory.getAbsolutePath()+ File.separator + "value-"+langPrefix);
		valueFolder.mkdir();
		String filePath = valueFolder.getAbsolutePath()+File.separator+"string.xml";
		System.out.println("Writing to file path " + filePath);
		File newFile = new File(filePath);
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		stringBuilder.append("<resources>\n");
		for(Map.Entry<String, String>  entry: tranlated.entrySet()){
			stringBuilder.append("<string name=\"" + entry.getKey()+"\">"+entry.getValue()+"</string>\n");
		}
		stringBuilder.append("</resources>\n");
		FileUtils.write(newFile, stringBuilder.toString(), "UTF-8");
	}

	private Map<String, String> readEntries()
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(new FileInputStream(originalLanguageFile), "UTF-8");

		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName("string");
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (int temp = 0; temp < nList.getLength(); temp++) {
			Node nNode = nList.item(temp);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				result.put(eElement.getAttribute("name"),
						eElement.getTextContent());
			}
		}
		return result;
	}

	protected Map<String, String> translate(
			Map<String, String> originalLanguageEntries, String dest) {
		getLog().info(
				"Proccessing language entries for " + originalLanguageEntries);
		Map<String, String> traslatedMap = new LinkedHashMap<String, String>();

		for (Map.Entry<String, String> entry : originalLanguageEntries
				.entrySet()) {

			try {
				String jsonString = httpGetJsonString(dest, entry.getValue());
				System.out.println("JSON STRING " + jsonString);
				Map<String, Object> map = new HashMap<String, Object>();
				map = new Gson().fromJson(jsonString, map.getClass());
				Map dataMap = (Map) map.get("data");
				List dataList = (List) dataMap.get("translations");
				Map transMap = (Map) dataList.get(0);
				String translatedText = (String) transMap.get("translatedText");
//				System.out.println(entry.getKey() + " : " + translatedText);
				traslatedMap.put(entry.getKey(), translatedText);
			} catch (IOException e) {
				rollback();
				throw new RuntimeException(e);
			}
		}
		return traslatedMap;
	}

	private void rollback() {
		// TODO Auto-generated method stub
	}

	private String httpGetJsonString(String destLanguge, String word)
			throws IOException {
		URL url = new URL(googleUrl + "?" + "key=" + googleKey + "&oe=UTF-8&target="
				+ destLanguge + "&q=" + URLEncoder.encode(word, "UTF-8"));
		URLConnection connection = url.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				connection.getInputStream(), "UTF-8"));
		String inputLine;
		String content = "";

		while ((inputLine = in.readLine()) != null)
			content += inputLine;
		in.close();
		return content;
	}


}
