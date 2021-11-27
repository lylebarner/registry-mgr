package tt;

import java.io.File;
import gov.nasa.pds.registry.mgr.dd.JsonLddLoader;


public class TestLddLoader
{
    
    public static void main(String[] args) throws Exception
    {
        JsonLddLoader loader = new JsonLddLoader("http://localhost:9200", "t1", null);
        loader.loadPds2EsDataTypeMap(new File("src/main/resources/elastic/data-dic-types.cfg"));

        //File ddFile = new File("src/test/data/PDS4_MSN_1B00_1100.JSON");
        //File ddFile = new File("/tmp/schema/PDS4_PDS_JSON_1F00.JSON");
        File lddFile = new File("/tmp/schema/PDS4_IMG_1F00_1810.JSON");        

        loader.load(lddFile, null);
        
        System.out.println("Done");
    }

}
