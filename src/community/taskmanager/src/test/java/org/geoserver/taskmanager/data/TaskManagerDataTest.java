package org.geoserver.taskmanager.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test data methods.
 *
 * @author Niels Charlier
 */
public class TaskManagerDataTest extends AbstractTaskManagerTest {

    @Autowired
    private TaskManagerDao dao;

    @Autowired
    private TaskManagerFactory fac;

    @Autowired
    private TaskManagerDataUtil util;

    private Configuration config;

    private Batch batch;

    @Before
    public void setupBatch() {
        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("some_ws");

        Task task = fac.createTask();
        task.setName("task");
        task.setType("testTask");
        util.addTaskToConfiguration(config, task);

        config = dao.save(config);

        batch = fac.createBatch();

        batch.setName("my_batch");
        batch = dao.save(batch);
    }

    @After
    public void clearDataFromDatabase() {
        dao.delete(batch);
        dao.delete(config);
    }

    @Test
    public void testBatchElement() {
        Task task = config.getTasks().get("task");
        BatchElement el = util.addBatchElement(batch, task);
        batch = dao.save(batch);
        assertEquals(1, batch.getElements().size());
        el = batch.getElements().get(0);
        //soft delete
        dao.remove(el);
        batch = dao.reload(batch);
        assertTrue(batch.getElements().isEmpty());
        BatchElement el2 = util.addBatchElement(batch, task);
        assertEquals(el.getId(), el2.getId());
        batch = dao.save(batch);
        assertEquals(1, batch.getElements().size());
        //hard delete
        dao.delete(batch.getElements().get(0));
        batch = dao.reload(batch);
        assertTrue(batch.getElements().isEmpty());
        el2 = util.addBatchElement(batch, task);
        assertFalse(el.getId().equals(el2.getId()));
    }

    @Test
    public void testCloneConfiguration() {
        Task task = util.init(config.getTasks().get("task"));
        assertEquals(0, task.getBatchElements().size());
        BatchElement el = util.addBatchElement(batch, task);
        batch = dao.save(batch);
        el = batch.getElements().get(0);
        task = util.init(task);
        assertEquals(1, task.getBatchElements().size());
        assertEquals(dao.reload(el), task.getBatchElements().get(0));

        Configuration config2 = dao.copyConfiguration("my_config");
        config2.setName("my_config2");
        config2 = dao.save(config2);
        task = util.init(config2.getTasks().get("task"));
        assertEquals(0, task.getBatchElements().size());

        dao.delete(config2);

        batch.setConfiguration(config);
        batch = dao.save(batch);

        config2 = dao.copyConfiguration("my_config");
        config2.setName("my_config2");
        config2 = dao.save(config2);
        task = util.init(config2.getTasks().get("task"));
        assertEquals(1, task.getBatchElements().size());
    }
}
