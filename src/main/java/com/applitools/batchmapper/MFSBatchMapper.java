package com.applitools.batchmapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.*;
import org.apache.pdfbox.pdmodel.PDDocument;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;

/**
 * 
 */

/**
 * @author levv
 *
 */
public class MFSBatchMapper {

	public static void main(String[] args) throws IOException {

		CommandLineParser parser = new DefaultParser();
		Options options = getOptions();

		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.getOptions().length == 0)  {
				usage(options);
				return;
			}

			MFSBatchMapperExceuter batchMapper = new MFSBatchMapper().new MFSBatchMapperExceuter(cmd);
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
                .desc("Aggregate all test results in a single batch (aka flat-batch), default: folder name ")
                .hasArg()
                .argName("name")
                .build());


		return options;

	}


	/**
	 * Print help.
	 */
	private static void usage(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("MFSBatch Mapper [-k <api-key>] [options]", options);
	}

	public class MFSBatchMapperExceuter
	{

		public int OFFSET = 2; // TOC + Header pages
		final String batchId =UUID.randomUUID().toString(); 
		
		private CommandLine cmd;  
		public MFSBatchMapperExceuter(CommandLine cmd)
		{
			this.cmd = cmd;
		}

		public void execute() throws IOException
		{

			File root = new File(cmd.getOptionValue("f", "."));
			if (root.isFile())
			{
				parseFile(root);
			}
			else
			{
				File[] files = root.listFiles();
				for (File file : files) {
					parseFile(file);
				}
			}

		} 


		private void parseFile(File file) throws IOException
		{
			ArrayList<String> TOC_Text = new ArrayList<String>();
			ArrayList<Integer> TOC_Pages = new ArrayList<Integer>();

			PDDocument pd = PDDocument.load(file);

			int totalPages = pd.getNumberOfPages();
			//		    System.out.println("Total Pages in Document: "+totalPages);

			ObjectExtractor oe = new ObjectExtractor(pd);
			BasicExtractionAlgorithm sea = new BasicExtractionAlgorithm();
			Page page = oe.extract(OFFSET);

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
			generateIT(TOC_Text,CalcPages(TOC_Pages,totalPages),file);

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

			totalPages=totalPages-OFFSET;
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
            
			String pattern = "java -jar ImageTester_2.2.1 %s %s -f \"%s\" -a \"%s\" -fn \"%s\" -sp %s -fb \"%s<>%s\" && \n";		  
			for(i=0;i<text.size();i++)
			{

				str+= String.format(pattern,apiKey,serverUrl,file.getCanonicalPath(),file.getName(), text.get(i),pages.get(i),batchName,batchId);

			}
			System.out.println(str.substring(0,str.lastIndexOf(" &&")));

		}
	}
}
