package gov.nasa.pds.registry.mgr.cmd.data;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.function.BiPredicate;

import org.apache.commons.cli.CommandLine;

import gov.nasa.pds.registry.common.es.dao.DataLoader;
import gov.nasa.pds.registry.mgr.Constants;
import gov.nasa.pds.registry.mgr.cfg.RegistryCfg;
import gov.nasa.pds.registry.mgr.cmd.CliCommand;
import gov.nasa.pds.registry.mgr.dao.RegistryManager;
import gov.nasa.pds.registry.mgr.dao.schema.SchemaUpdater;
import gov.nasa.pds.registry.mgr.util.ParamParserUtils;


/**
 * A CLI command to load PDS4 metadata into Registry. 
 * Multiple JSON files generated by Harvest are loaded into several 
 * Elasticsearch indices ("registry" and "registry-refs").
 * 
 * One of the files generated by Harvest, "fields.txt", contains a list 
 * of Elasticsearch field names extracted from PDS labels.
 * If any of these fields are missing in current Elasticsearch registry index schema,
 * this command will try to add missing fields to the schema.
 * 
 * @author karpenko
 */
public class LoadDataCmd implements CliCommand
{
    private RegistryCfg cfg;
    
    
    /**
     * Constructor
     */
    public LoadDataCmd()
    {
    }

    
    @Override
    public void run(CommandLine cmdLine) throws Exception
    {
        if(cmdLine.hasOption("help"))
        {
            printHelp();
            return;
        }

        cfg = new RegistryCfg();
        cfg.url = cmdLine.getOptionValue("es", "http://localhost:9200");
        cfg.indexName = cmdLine.getOptionValue("index", Constants.DEFAULT_REGISTRY_INDEX);
        cfg.authFile = cmdLine.getOptionValue("auth");

        String strDir = cmdLine.getOptionValue("dir");
        if(strDir == null) throw new Exception("Missing required parameter '-dir'");
        
        File dir = new File(strDir);
        if(!dir.exists() || !dir.isDirectory()) throw new Exception("Invalid directory " + dir.getAbsolutePath());
        
        String tmp = cmdLine.getOptionValue("updateSchema", "Y");
        boolean updateSchema = ParamParserUtils.parseYesNo("updateSchema", tmp);
        boolean fixMissingFDs = cmdLine.hasOption("force");
        
        RegistryManager.init(cfg);

        try
        {
            // Update schema
            if(updateSchema)
            {
                SchemaUpdater su = new SchemaUpdater(cfg, fixMissingFDs);
                su.updateLddsAndSchema(dir);
            }
            
            // Load data
            loadData(dir);
        }
        finally
        {
            RegistryManager.destroy();
        }
    }

    
    /**
     * Load data from JSON files generated by Harvest into "registry"
     * and "registry-refs" indices in Elasticsearch.
     * @param dir
     * @throws Exception
     */
    private void loadData(File dir) throws Exception
    {
        // Loader for main metadata ("registry" index)
        DataLoader registryLoader = new DataLoader(cfg.url, cfg.indexName, cfg.authFile);
        // Loader for references extracted from collection inventory files ("registry-refs" index)
        DataLoader refsLoader = new DataLoader(cfg.url, cfg.indexName + "-refs", cfg.authFile);
        refsLoader.setBatchSize(10);

        // Find all JSON files in the @param dir directory
        Iterator<Path> it = Files.find(dir.toPath(), 1, new JsonMatcher()).iterator();
        while(it.hasNext())
        {
            File file = it.next().toFile();
            String fileName = file.getName();
            if(fileName.startsWith("registry-docs"))
            {
                registryLoader.loadFile(file);
            }
            else if(fileName.startsWith("refs-docs"))
            {
                refsLoader.loadFile(file);
            }
            else
            {
                System.out.println("[WARN] " + "Unknown file type: " + file.getAbsolutePath());
            }
        }
    }
    

    /**
     * Inner class used by Files.find() to select all JSON files.
     * 
     * @author karpenko
     *
     */
    private static class JsonMatcher implements BiPredicate<Path, BasicFileAttributes>
    {
        @Override
        public boolean test(Path path, BasicFileAttributes attrs)
        {
            String fileName = path.getFileName().toString().toLowerCase();
            return fileName.endsWith(".json");
        }
    }

    
    /**
     * Print help screen.
     */
    public void printHelp()
    {
        System.out.println("Usage: registry-manager load-data <options>");

        System.out.println();
        System.out.println("Load data into registry index");
        System.out.println();
        System.out.println("Required parameters:");
        System.out.println("  -dir <path>           Harvest output directory to load"); 
        System.out.println("Optional parameters:");
        System.out.println("  -auth <file>          Authentication config file");
        System.out.println("  -es <url>             Elasticsearch URL. Default is http://localhost:9200");
        System.out.println("  -index <name>         Elasticsearch index name. Default is 'registry'");
        System.out.println("  -updateSchema <y/n>   Update registry schema. Default is 'yes'");
        System.out.println("  -force                Use 'keyword' ES datatype for missing field definitions.");

        System.out.println();
    }

}
