package com.applitools.MFSBatchMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.html.HTMLEditorKit.Parser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.pdfbox.pdmodel.PDDocument;

import com.opencsv.CSVWriter;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;

/**
 * 
 */

/**
 * @author Lev Volkovich
 *
 */
public class MFS_BatchMapper_Generator {

	public static void main(String[] args) throws IOException {

		CommandLineParser parser = new DefaultParser();
		Options options = getOptions();

		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.getOptions().length == 0)  {
				usage(options);
				return;
			}

			MFSBatchMapperExceuter batchMapper = new MFS_BatchMapper_Generator().new MFSBatchMapperExceuter(cmd);
			batchMapper.execute();
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Options getOptions() {
		Options options = new Options();

		options.addOption(Option.builder("k")
				.longOpt("apiKey")
				.desc("Applitools Apikey")
				.hasArg()
				.argName("apikey")
				.build());

		options.addOption(Option.builder("f")
				.longOpt("folder")
				.desc("Set the root folder to start the analysis, default: \\.")
				.hasArg()
				.argName("path")
				.build());

		options.addOption(Option.builder("s")
				.longOpt("server")
				.desc("Set Applitools server url, default: https://mfseyes.applitools.com")
				.hasArg()
				.argName("url")
				.build()
				);
		
        options.addOption(Option.builder("fb")
                .longOpt("flatbatch")
                .desc("Aggregate all test results in a single batch (aka flat-batch), default: \"Report Testing\" ")
                .hasArg()
                .argName("name")
                .build());

        options.addOption(Option.builder("t")
                .longOpt("toc")
                .desc("Table of Contents page number, default: 2 ")
                .hasArg()
                .argName("toc")
                .build());
        
        options.addOption(Option.builder("o")
                .longOpt("offset")
                .desc("Number of pages to offset for the page number count, default: TOC page number ")
                .hasArg()
                .argName("offset")
                .build());
        
        options.addOption(Option.builder("sc")
                .longOpt("section")
                .desc("Map only a specific section")
                .hasArg()
                .argName("section")
                .build());
        
        options.addOption(Option.builder("cg")
                .longOpt("cmdgen")
                .desc("Generate Commands in addition to csv")
                .hasArg(false)
                .argName("cmdgen")
                .build());


		return options;

	}


	/**
	 * Print help.
	 */
	private static void usage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MFS-BatchMapper-Generator [-k <api-key>] [options]", options);
	}

	public class MFSBatchMapperExceuter
	{

		final String batchId =UUID.randomUUID().toString();
		
		private int offset; // TOC + Header pages
		private int tocPage;
		private CommandLine cmd;  
		private String section;
		
		
		public MFSBatchMapperExceuter(CommandLine cmd)
		{
			this.cmd = cmd;
			this.tocPage = Integer.parseInt(cmd.getOptionValue("t", "2"));
			this.offset = Integer.parseInt(cmd.getOptionValue("o", ""+tocPage));
			this.section = cmd.getOptionValue("sc","");
		}

		public void execute() throws IOException
		{
			
			List<String[]> rows4CSV = new ArrayList<String[]>();
			CSVWriter writer = new CSVWriter(new FileWriter(System.getProperty("user.dir")+"/map.csv"),'|',CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
			
			//Add header row
			String[] headerRow = new String[] { "filePath", "testName", "app", "os", "browser", "viewport", "matchsize", "pages","matchLevel" };
			rows4CSV.add(headerRow);
			
			File root = new File(cmd.getOptionValue("f", "."));
			if (root.isFile())
			{
				parseFile(root);
			}
			else
			{
				File[] files = root.listFiles();
				for (File file : files) {
					rows4CSV.addAll(parseFile(file));
				}
			}
			
			writer.writeAll(rows4CSV);
			writer.close();
			String csvFilePath = System.getProperty("user.dir")+"/map.csv";
			System.out.println("Generated CSV file to: "+csvFilePath);
			System.out.println("Run the following command in ImageTester Folder");
			System.out.println(generateEndCommand(csvFilePath));						
		} 


		private List<String[]> parseFile(File file) throws IOException
		{
			ArrayList<String> TOC_Text = new ArrayList<String>();
			ArrayList<Integer> TOC_Pages = new ArrayList<Integer>();

			PDDocument pd = PDDocument.load(file);

			int totalPages = pd.getNumberOfPages();
			//		    System.out.println("Total Pages in Document: "+totalPages);

			ObjectExtractor oe = new ObjectExtractor(pd);
			BasicExtractionAlgorithm sea = new BasicExtractionAlgorithm();
			Page page = oe.extract(tocPage);

			// extract text from the table after detecting
			List<Table> table = sea.extract(page);
			for(Table tables: table) {
				List<List<RectangularTextContainer>> rows = tables.getRows();

				for(int i=0; i<rows.size(); i++) {

					List<RectangularTextContainer> cells = rows.get(i);

					for(int j=0; j<cells.size(); j++) {
						String text = cells.get(j).getText();
						//System.out.println(text);

						if(Pattern.matches("[a-zA-Z ]+\\d+$", text) && text.length()>1)
						{
							try {
								int len=text.length();
								TOC_Text.add(text.substring(0, text.lastIndexOf(" ")==-1? text.length()-1 :text.lastIndexOf(" ")));

								Pattern numbers = Pattern.compile("\\d+");
								Matcher matcher = numbers.matcher(text);
								if (matcher.find())
								{
									TOC_Pages.add(Integer.parseInt(matcher.group(0)));
								}

							}
							catch (Exception e) {
								// TODO: handle exception
								System.out.println(e.getMessage());
							}


						}



					}

				}
			}
			pd.close();
			
			if(cmd.hasOption("cmdgen"))
				generateIT(TOC_Text,CalcPages(TOC_Pages,totalPages),file);
			return generateList4CSV(TOC_Text,CalcPages(TOC_Pages,totalPages),file);

		}




		private ArrayList<String> CalcPages(ArrayList<Integer> pages,int totalPages)
		{
			ArrayList<String> TOC_pages = new ArrayList<String>();
			int loopSize= pages.size()-1;
			int i,a=0,b;

			for(i=0;i<loopSize;i++)
			{
				a=pages.get(i);
				b=pages.get(i+1);

				if(b-a==1)
				{
					TOC_pages.add(i, ""+a);
				}
				else
					TOC_pages.add(i, String.format("%d-%d", a,b-1));
			}

			totalPages=totalPages-offset;
			a=pages.get(i);

			if(totalPages-a==0)
			{
				TOC_pages.add(i, ""+a);
			}
			else
				TOC_pages.add(i, String.format("%d-%d", a,totalPages));

			return TOC_pages;

		}

		public void generateIT(ArrayList<String> text, ArrayList<String> pages,File file) throws IOException
		{
			int i;
			
			String str="";
            String apiKey = cmd.getOptionValue("k","");
            String serverUrl = cmd.getOptionValue("s", "https://mfseyes.applitools.com");
            String batchName = cmd.getOptionValue("fb", file.getParent().substring(file.getParent().lastIndexOf(File.separator)+1));
            
            
            apiKey = apiKey =="" ? "" : "-k " + apiKey;
            serverUrl = serverUrl =="" ? "" : "-s " + "\""+serverUrl+"\"";
            
			String pattern = "java -jar ImageTester_2.3.1.jar %s %s -f \"%s\" -a \"%s\" -fn \"%s\" -sp %s -fb \"%s<>%s\" && \n";		  
			for(i=0;i<text.size();i++)
			{
				if(text.get(i).compareTo(section) == 0 || section == "")
				str+= String.format(pattern,apiKey,serverUrl,file.getCanonicalPath(),file.getName(), text.get(i),pages.get(i),batchName,batchId);

			}
			System.out.println(str);

		}
		
		
		public String generateEndCommand(String csvPath) throws IOException
		{
			String str;
            String apiKey = cmd.getOptionValue("k","");
            String serverUrl = cmd.getOptionValue("s", "https://mfseyes.applitools.com");
            String batchName = cmd.getOptionValue("fb", "Report Testing");
            
            
            apiKey = apiKey =="" ? "" : "-k " + apiKey;
            serverUrl = serverUrl =="" ? "" : "-s " + "\""+serverUrl+"\"";
            
			String pattern = "java -jar ImageTester_2.3.1.jar %s %s -mp \"%s\" -fb \"%s<>%s\" -th 10";		  
			str= String.format(pattern,apiKey,serverUrl,csvPath,batchName,batchId);

			return str;
		}
		
		
		public List<String[]> generateList4CSV(ArrayList<String> text, ArrayList<String> pages,File file) throws IOException
		{
			int i;
			
			String str="";
			List<String[]> rows = new ArrayList<String[]>();
			String[] row;

			
            for(i=0;i<text.size();i++)
			{
				if(text.get(i).compareTo(section) == 0 || section == "")
				{
					
							
				row = new String[9];
				Arrays.fill(row, "");
				row[0]= file.getAbsolutePath();
				row[1]=text.get(i);  // TestName
				row[2]=file.getName(); // FileName as AppName
				row[7]=pages.get(i); // Span				
				
				rows.add(row);
				}
				

			}
			return rows;

		}
		
	}
}
