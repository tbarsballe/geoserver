package org.geoserver.taskmanager.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.sql.SQLException;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.schedule.TestTaskTypeImpl;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.Trigger.TriggerState;
import org.springframework.beans.factory.annotation.Autowired;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.decoder.RESTCoverage;

/**
 * To run this test you should have a geoserver running on http://localhost:9090/geoserver.
 *
 * @author Niels Charlier
 */
@Ignore
public class FileRemotePublicationTaskTest extends AbstractTaskManagerTest {

    private static final String ATT_LAYER = "layer";
    static final String ATT_EXT_GS = "geoserver";
    private static final String ATT_FAIL = "fail";

    @Autowired
    private LookupService<ExternalGS> extGeoservers;

    @Autowired
    private TaskManagerDao dao;

    @Autowired
    private TaskManagerFactory fac;

    @Autowired
    private TaskManagerDataUtil dataUtil;

    @Autowired
    private TaskManagerTaskUtil taskUtil;

    @Autowired
    private BatchJobService bjService;

    @Autowired
    private Scheduler scheduler;

    private Configuration config;

    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        DATA_DIRECTORY.addWcs11Coverages();
        return true;
    }

    @Before
    public void setupBatch() throws Exception {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(FileRemotePublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task1, FileRemotePublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(task1, FileRemotePublicationTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.addTaskToConfiguration(config, task1);

        config = dao.save(config);
        task1 = config.getTasks().get("task1");

        batch = fac.createBatch();

        batch.setName("my_batch");
        dataUtil.addBatchElement(batch, task1);

        batch = bjService.saveAndSchedule(batch);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(config);
    }

    @Test
    public void testSuccessAndCleanup() throws SchedulerException, SQLException, MalformedURLException {
        //set some metadata
        CoverageInfo ci = geoServer.getCatalog().getCoverageByName("DEM");
        ci.setTitle("my title ë");
        ci.setAbstract("my abstract ë");
        geoServer.getCatalog().save(ci);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "DEM");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        config = dao.save(config);

        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(batch.getFullName())
                .startNow()
                .build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.COMPLETE
                && scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {
        }

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertTrue(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertTrue(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertTrue(restManager.getReader().existsLayer("wcs", "DEM", true));

        RESTCoverage cov = restManager.getReader().getCoverage("wcs", "DEM", "DEM");
        assertEquals(ci.getTitle(), cov.getTitle());
        assertEquals(ci.getAbstract(), cov.getAbstract());

        assertTrue(taskUtil.cleanup(config));

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }

    @Test
    public void testRollback() throws SchedulerException, SQLException, MalformedURLException {
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(TestTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, TestTaskTypeImpl.PARAM_FAIL, ATT_FAIL);
        dataUtil.addTaskToConfiguration(config, task2);

        dataUtil.setConfigurationAttribute(config, ATT_LAYER, "DEM");
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");
        dataUtil.setConfigurationAttribute(config, ATT_FAIL, Boolean.TRUE.toString());
        config = dao.save(config);
        task2 = config.getTasks().get("task2");
        dataUtil.addBatchElement(batch, task2);
        batch = bjService.saveAndSchedule(batch);

        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(batch.getFullName())
                .startNow()
                .build();
        scheduler.scheduleJob(trigger);

        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.COMPLETE
                && scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {
        }

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();

        assertFalse(restManager.getReader().existsCoveragestore("wcs", "DEM"));
        assertFalse(restManager.getReader().existsCoverage("wcs", "DEM", "DEM"));
        assertFalse(restManager.getReader().existsLayer("wcs", "DEM", true));
    }

}
