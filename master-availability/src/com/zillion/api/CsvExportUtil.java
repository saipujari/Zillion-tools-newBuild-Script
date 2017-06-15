package com.zillion.api;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.joda.time.DateTime;

public class CsvExportUtil {

	public static void exportCsv(Map<String,StringBuilder> exportValue,String destinationPath,String fileName) throws IOException{
		DateTime st = new DateTime();
		for(String exportKeys : exportValue.keySet()){
			 FileWriter writer = new FileWriter(destinationPath+"/"+fileName+exportKeys+".csv");
			 writer.write(exportValue.get(exportKeys).toString());
			 writer.flush();
		     writer.close();
		}
		DateTime et = new DateTime();
		  System.out.println("Time taken to ExportCSV " +(et.getMillis()-st.getMillis()) +"ms");
		  MasterAvailabilityReportHelper.timeTaken = MasterAvailabilityReportHelper.timeTaken +(et.getMillis()-st.getMillis());
	}
}
