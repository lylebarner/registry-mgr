package tt;

import java.io.File;

import gov.nasa.pds.registry.common.es.dao.dd.DataDictionaryDao;
import gov.nasa.pds.registry.common.es.service.JsonLddLoader;
import gov.nasa.pds.registry.mgr.cfg.RegistryCfg;
import gov.nasa.pds.registry.mgr.dao.RegistryManager;


public class TestLddLoader
{
    
    public static void main(String[] args) throws Exception
    {
        RegistryCfg cfg = new RegistryCfg();
        cfg.url = "http://localhost:9200";
        cfg.indexName = "registry";
        
        try
        {
            RegistryManager.init(cfg);
            
            DataDictionaryDao ddDao = RegistryManager.getInstance().getDataDictionaryDao();
            JsonLddLoader loader = new JsonLddLoader(ddDao, "http://localhost:9200", "t1", null);
            loader.loadPds2EsDataTypeMap(new File("src/main/resources/elastic/data-dic-types.cfg"));
    
            //File ddFile = new File("src/test/data/PDS4_MSN_1B00_1100.JSON");
            //File ddFile = new File("/tmp/schema/PDS4_PDS_JSON_1F00.JSON");
            File lddFile = new File("/tmp/schema/PDS4_IMG_1F00_1810.JSON");        
    
            loader.load(lddFile, null);
            
            System.out.println("Done");
        }
        finally
        {
            RegistryManager.destroy();
        }
    }

}
