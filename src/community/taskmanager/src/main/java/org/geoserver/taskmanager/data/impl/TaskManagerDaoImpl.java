/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Identifiable;
import org.geoserver.taskmanager.data.Parameter;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.SoftRemove;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Repository
@Transactional
public class TaskManagerDaoImpl implements TaskManagerDao {

    @Autowired
    private SessionFactory sf;

    public final Session getSession() {
        Session session = sf.getCurrentSession();
        session.enableFilter("activeTaskFilter");
        session.enableFilter("activeBatchFilter");
        session.enableFilter("activeElementFilter");
        session.enableFilter("activeTaskElementFilter");
        return session;
    }

    @SuppressWarnings("unchecked")
    protected <T> T saveObject(T o) {
        o = (T) getSession().merge(o);
        getSession().flush();
        return o;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Identifiable> T reload(T object) {
        return (T) getSession().get(object.getClass(), object.getId());
    }

    @Override
    public Run save(final Run run) {
        return saveObject(run);
    }

    @Override
    public Configuration save(final Configuration config) {
        return saveObject(config);
    }

    @Override
    public Batch save(final Batch batch) {
        int i = 0;
        for (BatchElement element : batch.getElements()) {
            if (element.isActive()) {
                element.setIndex(i++);
            } else {
                element.setIndex(null);
            }
        }
        return saveObject(batch);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Batch> getBatches() {
        return getSession().createCriteria(BatchImpl.class)
                .createAlias("configuration", "configuration", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.eq("removeStamp", 0L))
                .add(Restrictions.or(
                        Restrictions.isNull("configuration"),
                        Restrictions.and(
                                Restrictions.eq("configuration.removeStamp", 0L),
                                Restrictions.eq("configuration.template", false)
                        )
                )).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Configuration> getConfigurations(Boolean templates) {
        Criteria criteria = getSession().createCriteria(ConfigurationImpl.class)
                .add(Restrictions.eq("removeStamp", 0L));
        if (templates != null) {
            criteria.add(Restrictions.eq("template", templates));
        }
        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
    }

    @Override
    public Configuration getConfiguration(long id) {
        return (Configuration) getSession().get(ConfigurationImpl.class, id);
    }

    @Override
    public Batch getBatch(long id) {
        return (Batch) getSession().get(BatchImpl.class, id);
    }

    @Override
    public Configuration getConfiguration(final String name) {
        return (Configuration) getSession().createCriteria(ConfigurationImpl.class)
                .add(Restrictions.eq("removeStamp", 0L))
                .add(Restrictions.eq("name", name)).uniqueResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Task> getTasksAvailableForBatch(Batch batch) {
        DetachedCriteria alreadyInBatch = DetachedCriteria.forClass(BatchElementImpl.class)
                .createAlias("batch", "batch")
                .createAlias("task", "task")
                .add(Restrictions.eq("batch.id", batch.getId()))
                .setProjection(Projections.property("task.id"));
        Criteria criteria = getSession().createCriteria(TaskImpl.class)
                .createAlias("configuration", "configuration")
                .add(Restrictions.eq("removeStamp", 0L))
                .add(Restrictions.eq("configuration.removeStamp", 0L))
                .add(Subqueries.propertyNotIn("id", alreadyInBatch));

        if (batch.getConfiguration() == null) {
            criteria.add(Restrictions.eq("configuration.template", false));
        } else {
            criteria.add(Restrictions.eq("configuration.id", batch.getConfiguration().getId()));
        }

        return (List<Task>) criteria.list();
    }

    @Override
    public Batch getBatch(final String fullName) {
        String[] splitName = fullName.split(Batch.FULL_NAME_DIVISOR, 2);
        Criteria criteria = getSession().createCriteria(BatchImpl.class)
                .add(Restrictions.eq("removeStamp", 0L));

        if (splitName.length > 1) {
            criteria.createAlias("configuration", "configuration")
                    .add(Restrictions.eq("configuration.name", splitName[0]))
                    .add(Restrictions.eq("name", splitName[1]))
                    .add(Restrictions.eq("removeStamp", 0L))
                    .add(Restrictions.eq("configuration.removeStamp", 0L));
        } else {
            criteria.add(Restrictions.isNull("configuration"))
                    .add(Restrictions.eq("name", splitName[0]))
                    .add(Restrictions.eq("removeStamp", 0L));
        }

        return (Batch) criteria.uniqueResult();
    }

    @Override
    public BatchElement getBatchElement(final Batch batch, final Task task) {
        return (BatchElement) getSession().createCriteria(BatchElementImpl.class)
                .createAlias("batch", "batch")
                .createAlias("task", "task")
                .add(Restrictions.eq("batch.id", batch.getId()))
                .add(Restrictions.eq("task.id", task.getId()))
                .uniqueResult();
    }

    @Override
    public <T extends SoftRemove> T remove(T item) {
        item.setActive(false);
        return saveObject(item);
    }

    @Override
    public Run getCurrentRun(final Task task) {
        return (Run) (getSession().createCriteria(RunImpl.class).setLockMode(LockMode.PESSIMISTIC_READ)
                .createAlias("batchElement", "batchElement")
                .createAlias("batchElement.task", "task")
                .add(Restrictions.eq("task.id", task.getId()))
                .add(Restrictions.isNull("end"))).uniqueResult();
    }

    @Override
    public Run getCommittingRun(final Task task) {
        return (Run) (getSession().createCriteria(RunImpl.class).setLockMode(LockMode.PESSIMISTIC_READ)
                .createAlias("batchElement", "batchElement")
                .createAlias("batchElement.task", "task")
                .add(Restrictions.eq("task.id", task.getId()))
                .add(Restrictions.isNotNull("end"))
                .add(Restrictions.eq("status", Run.Status.COMMITTING))).uniqueResult();
    }

    @Override
    public void delete(Batch batch) {
        batch = (Batch) getSession().get(BatchImpl.class, batch.getId());
        if (batch.getConfiguration() != null) {
            batch.getConfiguration().getBatches().remove(batch.getName());
        }
        getSession().delete(batch);
    }

    @Override
    public void delete(Configuration config) {
        getSession().delete(getSession().get(ConfigurationImpl.class, config.getId()));
    }

    @Override
    public void delete(BatchElement batchElement) {
        batchElement = (BatchElement) getSession().get(BatchElementImpl.class, batchElement.getId());
        batchElement.getBatch().getElements().remove(batchElement);
        getSession().delete(batchElement);
    }

    @Override
    public void delete(Task task) {
        task = (Task) getSession().get(TaskImpl.class, task.getId());
        task.getConfiguration().getTasks().remove(task.getName());
        getSession().delete(task);
    }

    @Override
    public Run getLatestRun(BatchElement batchElement) {
        return (Run) (getSession().createCriteria(RunImpl.class)
                .createAlias("batchElement", "batchElement")
                .add(Restrictions.eq("batchElement.id", batchElement.getId()))
                .addOrder(Order.desc("start")))
                .setMaxResults(1).uniqueResult();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Configuration copyConfiguration(String configName) {
        ConfigurationImpl clone = (ConfigurationImpl) getConfiguration(configName);
        getSession().evict(clone);
        clone.setId(null);
        for (Attribute att : clone.getAttributes().values()) {
            att.setConfiguration(clone);
            ((AttributeImpl) att).setId(null);
        }
        for (Task task : clone.getTasks().values()) {
            task.setConfiguration(clone);
            ((TaskImpl) task).setId(null);
            ((TaskImpl) task).setBatchElements(new ArrayList<BatchElement>());
            for (Parameter param : task.getParameters().values()) {
                param.setTask(task);
                ((ParameterImpl) param).setId(null);
            }
        }
        for (Batch batch : clone.getBatches().values()) {
            batch.setConfiguration(clone);
            ((BatchImpl) batch).setId(null);
            for (BatchElement be : batch.getElements()) {
                be.setBatch(batch);
                be.setTask(clone.getTasks().get(be.getTask().getName()));
                ((BatchElementImpl) be).setId(null);
                if (Hibernate.isInitialized(be.getRuns())) {
                    be.getRuns().clear();
                }
            }
            if (Hibernate.isInitialized(batch.getBatchRuns())) {
                batch.getBatchRuns().clear();
            }
        }
        return clone;
    }

}
