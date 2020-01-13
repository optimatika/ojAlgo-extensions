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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.ojalgo.type.keyvalue.StringToObject;

/**
 * AccessManager (DAO)
 *
 * @author apete
 */
public abstract class AccessManager<EB extends Object, BO extends AbstractBO<EB>> {

    protected AccessManager() {
        super();
    }

    public final BO attach(final BO unattached, final EditingContext editingContext) {
        return this.findUsingPrimaryKey(editingContext, this.getPrimaryKeyValue(unattached));
    }

    public final void detach(final BO attached, final EditingContext editingContext) {
        editingContext.detach(attached);
    }

    @SuppressWarnings("unchecked")
    public final List<BO> fetchAll(final EditingContext editingContext) {

        final Query tmpQuery = editingContext.createQuery("SELECT t FROM " + this.getEntityBeanClass().getSimpleName() + " t");
        List<EB> tmpResults = null;

        try {
            tmpResults = tmpQuery.getResultList();
        } catch (final Exception anException) {

        }

        final ArrayList<BO> retVal = new ArrayList<>();

        editingContext.wrap(tmpResults, retVal, this);

        return retVal;
    }

    public final BO fetchOne(final EditingContext editingContext, final String key, final Object value) {

        EB retVal = null;

        if ((key == null) && (value != null)) {
            final BO tmpFindUsingPrimaryKey = this.findUsingPrimaryKey(editingContext, value);
            if (tmpFindUsingPrimaryKey != null) {
                retVal = tmpFindUsingPrimaryKey.getEntityBean();
            } else {
                retVal = null;
            }
        }

        if ((retVal == null) && (key != null) && (value != null)) {
            final List<EB> tmpResults = this.queryUsingParameters(editingContext, new StringToObject<>(key, value));
            if ((tmpResults != null) && (tmpResults.size() >= 1)) {
                if (tmpResults.size() > 1) {
                    throw new IllegalArgumentException();
                } else {
                    retVal = tmpResults.get(0);
                }
            }
        }

        if (retVal != null) {
            return editingContext.wrap(retVal, this);
        } else {
            return null;
        }
    }

    public final BO fetchOrCreate(final EditingContext editingContext, final Object... parameters) {

        final StringToObject<?>[] tmpParameters = new StringToObject<?>[parameters.length];
        for (int i = 0; i < tmpParameters.length; i++) {
            tmpParameters[i] = this.deduceKeyFor(parameters[i]);
        }

        final List<EB> tmpQueryResults = this.queryUsingParameters(editingContext, tmpParameters);

        if ((tmpQueryResults == null) || (tmpQueryResults.size() == 0)) {
            // Create...

            final EB tmpEntityBean = this.newEntityBean();

            try {

                final BeanInfo tmpEntityInfo = Introspector.getBeanInfo(this.getEntityBeanClass());
                final PropertyDescriptor[] tmpEntityProperties = tmpEntityInfo.getPropertyDescriptors();

                for (final StringToObject<?> tmpPropertyValuePair : tmpParameters) {
                    for (final PropertyDescriptor tmpPropertyDescriptor : tmpEntityProperties) {
                        if (tmpPropertyDescriptor.getName().equals(tmpPropertyValuePair.key)) {
                            tmpPropertyDescriptor.getWriteMethod().invoke(tmpEntityBean, tmpPropertyValuePair.value);
                            break;
                        }
                    }
                }

            } catch (final Exception anException) {

                throw new IllegalStateException(anException);
            }

            final BO tmpBusinessObject = this.wrap(editingContext, tmpEntityBean);

            editingContext.insert(tmpBusinessObject);

            return tmpBusinessObject;

        } else if (tmpQueryResults.size() >= 2) {
            // Should not happen
            throw new IllegalStateException();

        } else {
            // Found precisely 1
            return editingContext.wrap(tmpQueryResults.get(0), this);
        }
    }

    public final List<BO> fetchSet(final EditingContext editingContext, final StringToObject<?>... parameters) {

        final List<EB> tmpVal = this.queryUsingParameters(editingContext, parameters);

        if (tmpVal != null) {

            final ArrayList<BO> retVal = new ArrayList<>();

            editingContext.wrap(tmpVal, retVal, this);

            return retVal;

        } else {

            return null;
        }
    }

    public final BO findUsingPrimaryKey(final EditingContext editingContext, final Object primaryKeyValue) {
        final Class<EB> tmpBeanClass = this.getEntityBeanClass();
        final EB tmpEntityBean = editingContext.find(tmpBeanClass, primaryKeyValue);
        if (tmpEntityBean != null) {
            return editingContext.wrap(tmpEntityBean, this);
        } else {
            return null;
        }
    }

    public abstract Object getPrimaryKeyValue(BO businessObject);

    public BO newInstance(final EditingContext editingContext) {

        final EB tmpEntityBean = this.newEntityBean();

        return this.wrap(editingContext, tmpEntityBean);
    }

    private EB newEntityBean() {
        EB tmpBean = null;

        try {
            final Constructor<EB> tmpConstructor = this.getEntityBeanClass().getConstructor();
            tmpConstructor.setAccessible(true);
            tmpBean = tmpConstructor.newInstance();
        } catch (final Exception anException) {
            anException.printStackTrace();
        }
        return tmpBean;
    }

    protected abstract StringToObject<?> deduceKeyFor(Object parameter);

    protected abstract Class<EB> getEntityBeanClass();

    @SuppressWarnings("unchecked")
    protected final List<EB> queryUsingParameters(final EditingContext editingContext, final StringToObject<?>... parameters) {

        final StringBuilder tmpQueryString = new StringBuilder("SELECT t FROM ");
        tmpQueryString.append(this.getEntityBeanClass().getSimpleName());
        if (parameters.length > 0) {
            tmpQueryString.append(" t WHERE t.");
            tmpQueryString.append(parameters[0].key);
            tmpQueryString.append(" = :");
            tmpQueryString.append(parameters[0].key);
            for (int i = 1; i < parameters.length; i++) {
                tmpQueryString.append(" AND t.");
                tmpQueryString.append(parameters[i].key);
                tmpQueryString.append(" = :");
                tmpQueryString.append(parameters[i].key);
            }
        }

        final Query tmpQuery = editingContext.createQuery(tmpQueryString.toString());
        for (final StringToObject<?> tmpStringToObject : parameters) {
            tmpQuery.setParameter(tmpStringToObject.key, tmpStringToObject.value);
        }

        List<EB> retVal = null;

        try {
            retVal = tmpQuery.getResultList();
        } catch (final Exception anException) {

        }

        return retVal;
    }

    protected abstract BO wrap(EditingContext editingContext, Object entityBean);

}
