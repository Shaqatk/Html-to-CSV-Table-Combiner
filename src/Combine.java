import java.io.*;
import java.util.*;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import org.apache.commons.io.FilenameUtils;

public class Combine {
  
    private Table<String, String, String> table;
    private String directory;

    public Combine() throws IOException{
        table = TreeBasedTable.create(Comparator.naturalOrder(), id_col);
        directory = "";
    }

    public Combine(String filedir){
        table = TreeBasedTable.create(Comparator.naturalOrder(), id_col);
        directory = filedir;
    }
   
    public Table<String, String, String> combine_name
            (String[] f_name) throws IOException{
        for (String f : f_name){
            switch(FilenameUtils.getExtension(f)) {
                case "csv":
                    CSV_file(f);
                    break;
                case "html":
                    HTML_file(f);
                    break;
                default:
                    break;
            }
        }
        return table;
    }

    private void CSV_file(String csvfile) throws IOException{
        CSVReader reader = new CSVReader(
                new FileReader(directory + csvfile),',' , '"' , 0);
        int size = -1;
        int x = -1;
        String[] nextLine;
        Map<Integer, String> map = new HashMap<>();
        
        if ((nextLine = reader.readNext()) != null){
            size = nextLine.length;
            for (int i = 0; i < size; i++){
                if ("ID".equals(nextLine[i])) {
                    x = i;
                }
                map.put(i, nextLine[i]);
            }
            if (x == -1){
                System.err.println(csvfile + "no ID");
                System.exit(1);
            }
        }

        while ((nextLine = reader.readNext()) != null) {
            
            if (nextLine.length != size) {
                System.err.println(csvfile + "table is invalid");
                System.exit(1);
            }
            String id = nextLine[x];
            if (!id_validation(id)){
                System.err.println(csvfile + "invalid id");
                System.exit(1);
            }
            for (int i = 0; i < size; i++) {
                synchronized (this) {
                    if (table.get(id, map.get(i)) == null) {
                        table.put(id, map.get(i),
                                nextLine[i]);
                    }
                    }
                }
            }
        	reader.close();
        }

    private void HTML_file(String htmlfile) throws IOException{
        File in = new File(directory + htmlfile);
        Document doc = Jsoup.parse(in, null);
        Elements col = doc.select("#directory tr th");
        int size = col.size();
        int x = -1;
        Map<Integer, String> map = new HashMap<>();

        for (int i = 0; i < size; i++){
            // identify the index of ID
            if ("ID".equals(col.get(i).text())){
                x = i;
            }
            map.put(i, col.get(i).text());

        }
       
        if (x == -1){
            System.err.println(htmlfile + "no ID");
            System.exit(1);
        }

        Elements td = doc.select("#directory tr td");
       
        if (td.size()%size != 0){
            System.err.println(htmlfile + "table is invalid");
            System.exit(1);
        }
        // populate html table data into table
        for(int i = 0; i < td.size(); i += size){
            int indexId = size*(i/size) + x;
            String id = td.get(indexId).text();
            // ERROR CHECKING: ID not valid
            if (!id_validation(id)){
                System.err.println(htmlfile + "invalid ID");
                System.exit(1);
            }
            for (int j = i; j < i + size; j++) {
                synchronized (this) {
                    if (table.get(id, map.get(j % size)) == null) {
                        table.put(id, map.get(j % size),
                                td.get(j).text());
                    }
                        }
                    }
                }
            }

    public void converter(Table<String, String, String> table) throws IOException{
        String out_file = "combined.csv";
        CSVWriter writer = new CSVWriter(new FileWriter(out_file),
                ',','"');
        Set<String> cols = table.columnKeySet();
        writer.writeNext(cols.toArray(new String[cols.size()]));
        for(String id : table.rowKeySet()){
            List<String> row = new ArrayList<>();
            for(String col : table.columnKeySet()){
                row.add(table.get(id, col)
                        == null ? "" : table.get(id, col));
            }
            writer.writeNext(row.toArray(
                        new String[row.size()]));
        }
        writer.close();
    }

    private Comparator<String> id_col = new Comparator<String>() {
        
        public int compare(String x, String y) {
            if ("ID".equals(x) && "ID".equals(y)){
                return 0;
            }else if ("ID".equals(x)){
                return -1;
            }else if ("ID".equals(y)){
                return 1;
            }else{
                return x.compareTo(y);
            }
        }
    };

    private boolean id_validation(String id){
        return !(id == null || "".equals(id.replaceAll("\\s+","")));
    }

}