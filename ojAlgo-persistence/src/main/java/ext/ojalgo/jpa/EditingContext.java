/*
 * Copyright 1997-2020 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ext.ojalgo.jpa;

import java.io.Serializable;
import java.util.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.ojalgo.netio.BasicLogger;

/**
 * Handles {@link AbstractBO} changes.
 *
 * @author apete
 */
public class EditingContext extends Object implements Serializable {

    public static final boolean DEBUG = false;

    private static EntityManagerFactory EMF = null;

    /**
     * Will only work if the required connection parameters are defined in persistence.xml
     */
    public static void configure(String persistenceUnitName) {
        EMF = Persistence.createEntityManagerFactory(persistenceUnitName);
    }

    /**
     * @param properties Must contain the connection parameters (get them from some properties file)
     */
    public static void configure(String persistenceUnitName, Properties properties) {
        EMF = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
    }

    private final HashMap<Object, AbstractBO<?>> myAllObjects = new HashMap<>();
    private final LinkedList<AbstractBO<?>> myDeletes = new LinkedList<>();
    private final EntityManager myEntityManager = EMF.createEntityManager();
    private final UUID myIdentifier = UUID.randomUUID();
    private final LinkedList<AbstractBO<?>> myInserts = new LinkedList<>();
    private final Set<AbstractBO<?>> myRefreshs = new HashSet<>();

    private final Set<AbstractBO<?>> myUpdates = new HashSet<>();

    public EditingContext() {
        super();
    }

    public void addDeleteFirst(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        myDeletes.addFirst(aBO);
    }

    public void addDeleteLast(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        myDeletes.addLast(aBO);
    }

    public void addInsertFirst(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        myInserts.addFirst(aBO);
    }

    public void addInsertLast(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        myInserts.addLast(aBO);
    }

    public void addRefresh(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        myRefreshs.add(aBO);
    }

    public void addRefresh(final Collection<? extends AbstractBO<?>> someBOs) {
        for (final AbstractBO<?> tmpBO : someBOs) {
            this.addRefresh(tmpBO);
        }
    }

    public void addUpdate(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        myUpdates.add(aBO);
    }

    public void addUpdate(final Collection<? extends AbstractBO<?>> someBOs) {
        for (final AbstractBO<?> tmpBO : someBOs) {
            this.addUpdate(tmpBO);
        }
    }

    public <EB extends Object, BO extends AbstractBO<EB>> BO attach(final BO businessObject, final AccessManager<EB, BO> accessManager) {
        return accessManager.attach(businessObject, this);
    }

    public void commit() {

        final EntityManager tmpEntityManager = myEntityManager;
        final EntityTransaction tmpTransaction = tmpEntityManager.getTransaction();

        for (final AbstractBO<?> tmpBO : this.copy(myInserts)) {
            tmpBO.appendAdditionalInserts(this);
        }
        for (final AbstractBO<?> tmpBO : this.copy(myDeletes)) {
            tmpBO.prependAdditionalDeletes(this);
        }

        if (DEBUG) {
            BasicLogger.debug("Inserts: {}", myInserts);
            BasicLogger.debug("Updates: {}", myUpdates);
            BasicLogger.debug("Deletes: {}", myDeletes);
            BasicLogger.debug("Refreshs: {}", myRefreshs);
        }

        tmpTransaction.begin();

        for (final AbstractBO<?> tmpBO : myInserts) {
            tmpEntityManager.persist(tmpBO.getEntityBean());
        }
        for (final AbstractBO<?> tmpBO : myUpdates) {
            tmpEntityManager.merge(tmpBO.getEntityBean());
        }
        for (final AbstractBO<?> tmpBO : myDeletes) {
            tmpEntityManager.remove(tmpBO.getEntityBean());
        }

        try {

            tmpTransaction.commit();

            for (final AbstractBO<?> tmpBO : myInserts) {
                this.putDataAccessObject(tmpBO.getEntityBean(), tmpBO);
            }

            tmpTransaction.begin();

            for (final AbstractBO<?> tmpBO : myRefreshs) {
                final Object tmpEntityBean = tmpBO.getEntityBean();
                if (tmpEntityManager.contains(tmpEntityBean)) {
                    tmpEntityManager.refresh(tmpEntityBean);
                }
            }

            try {

                tmpTransaction.commit();

            } catch (final Exception anException) {

                BasicLogger.error(anException.toString());

                if ((tmpTransaction != null) && tmpTransaction.isActive()) {
                    tmpTransaction.rollback();
                }
            }

        } catch (final Exception anException) {

            BasicLogger.error(anException.toString());

            if ((tmpTransaction != null) && tmpTransaction.isActive()) {
                tmpTransaction.rollback();
            }

        } finally {

            myInserts.clear();
            myUpdates.clear();
            myDeletes.clear();
            myRefreshs.clear();

            this.flushCaches();
        }
    }

    public boolean contains(final AbstractBO<?> aBO) {
        return myAllObjects.containsKey(aBO.getEntityBean());
    }

    public Query createQuery(final String string) {
        return myEntityManager.createQuery(string);
    }

    public void delete(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        final EntityManager tmpEntityManager = myEntityManager;
        final EntityTransaction tmpTransaction = tmpEntityManager.getTransaction();

        aBO.prependAdditionalDeletes(this);

        tmpTransaction.begin();

        tmpEntityManager.remove(aBO.getEntityBean());

        try {

            tmpTransaction.commit();

        } catch (final Exception anException) {

            BasicLogger.error(anException.toString());

            if ((tmpTransaction != null) && tmpTransaction.isActive()) {
                tmpTransaction.rollback();
            }

        } finally {

            this.flushCaches();
        }
    }

    public void detach(final AbstractBO<?> businessObject) {
        final Object tmpEntityBean = businessObject.getEntityBean();
        myAllObjects.remove(tmpEntityBean);
        myEntityManager.detach(tmpEntityBean);
    }

    @Override
    public final boolean equals(final Object someObj) {
        if (someObj instanceof EditingContext) {
            return myIdentifier.equals(((EditingContext) someObj).getIdentifier());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return myIdentifier.hashCode();
    }

    public void insert(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        final EntityManager tmpEntityManager = myEntityManager;
        final EntityTransaction tmpTransaction = tmpEntityManager.getTransaction();

        aBO.appendAdditionalInserts(this);

        tmpTransaction.begin();

        tmpEntityManager.persist(aBO.getEntityBean());

        try {

            tmpTransaction.commit();

            this.putDataAccessObject(aBO.getEntityBean(), aBO);

        } catch (final Exception anException) {

            BasicLogger.error(anException.toString());

            if ((tmpTransaction != null) && tmpTransaction.isActive()) {
                tmpTransaction.rollback();
            }

        } finally {

            this.flushCaches();
        }
    }

    public void refresh(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        final EntityManager tmpEntityManager = myEntityManager;
        final EntityTransaction tmpTransaction = tmpEntityManager.getTransaction();

        tmpTransaction.begin();

        tmpEntityManager.refresh(aBO.getEntityBean());

        try {

            tmpTransaction.commit();

        } catch (final Exception anException) {

            BasicLogger.error(anException.toString());

            if ((tmpTransaction != null) && tmpTransaction.isActive()) {
                tmpTransaction.rollback();
            }

        } finally {

            this.flushCaches();
        }
    }

    /**
     * Will perform a rollback, invalidate all contained DAOs and clear all references to any DAO. When this
     * is done the EC is ready to be either disposed or re-used. The DAOs that where previously used with this
     * EC are invalid, and can not be used for anything.
     */
    public void reset() {

        myEntityManager.clear();

        this.rollback();

        myAllObjects.clear();
    }

    public void rollback() {

        myInserts.clear();
        myUpdates.clear();
        myDeletes.clear();
        myRefreshs.clear();

        this.flushCaches();
    }

    public void update(final AbstractBO<?> aBO) {

        if (!aBO.getEditingContext().equals(this)) {
            throw new IllegalStateException();
        }

        final EntityManager tmpEntityManager = myEntityManager;
        final EntityTransaction tmpTransaction = tmpEntityManager.getTransaction();

        tmpTransaction.begin();

        tmpEntityManager.merge(aBO.getEntityBean());

        try {

            tmpTransaction.commit();

        } catch (final Exception anException) {

            BasicLogger.error(anException.toString());

            if ((tmpTransaction != null) && tmpTransaction.isActive()) {
                tmpTransaction.rollback();
            }

        } finally {

            this.flushCaches();
        }
    }

    public <EB, BO extends AbstractBO<EB>> List<BO> wrap(final Collection<EB> entityBeans, final List<BO> businessObjectContainer,
            final AccessManager<EB, BO> businessObjectManager) {

        businessObjectContainer.clear();

        if ((entityBeans != null) && (entityBeans.size() > 0)) {

            for (final EB tmpEntityBean : entityBeans) {
                final BO tmpBusinessObject = this.wrap(tmpEntityBean, businessObjectManager);
                businessObjectContainer.add(tmpBusinessObject);
            }

            Collections.sort(businessObjectContainer);
        }

        return businessObjectContainer;
    }

    @SuppressWarnings("unchecked")
    public <EB, BO extends AbstractBO<EB>> BO wrap(final EB entityBean, final AccessManager<EB, BO> businessObjectManager) {

        if (entityBean != null) {

            BO retVal = (BO) myAllObjects.get(entityBean);

            if (retVal == null) {
                retVal = businessObjectManager.wrap(this, entityBean);
                this.putDataAccessObject(entityBean, retVal);
            }

            return retVal;

        } else {

            return null;
        }
    }

    private void flushCaches() {
        for (final AbstractBO<?> tmpBO : myAllObjects.values()) {
            tmpBO.flushCaches();
        }
    }

    protected <T> T find(final Class<T> arg0, final Object arg1) {
        return myEntityManager.find(arg0, arg1);
    }

    List<AbstractBO<?>> copy(final Collection<? extends AbstractBO<?>> collection) {
        return Collections.unmodifiableList(new ArrayList<AbstractBO<?>>(collection));
    }

    UUID getIdentifier() {
        return myIdentifier;
    }

    void invalidate(final AbstractBO<?> aBO) {

        myUpdates.remove(aBO);
        myDeletes.remove(aBO);

        myAllObjects.remove(aBO.getEntityBean());
    }

    boolean putDataAccessObject(final Object key, final AbstractBO<?> value) {
        return myAllObjects.put(key, value) == null;
    }

}
