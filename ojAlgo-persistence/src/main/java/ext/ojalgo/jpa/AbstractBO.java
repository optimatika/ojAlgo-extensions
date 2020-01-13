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

/**
 * <p>
 * BusinessObject
 * </p>
 * <p>
 * The BusinessObject represents the data client. It is the object that requires access to the data source to
 * obtain and store data. A BusinessObject may be implemented as a session bean, entity bean, or some other
 * Java object, in addition to a servlet or helper bean that accesses the data source.
 * </p>
 * <p>
 * DataAccessObject
 * </p>
 * <p>
 * The DataAccessObject is the primary object of this pattern. The DataAccessObject abstracts the underlying
 * data access implementation for the BusinessObject to enable transparent access to the data source. The
 * BusinessObject also delegates data load and store operations to the DataAccessObject.
 * </p>
 * <p>
 * DataSource
 * </p>
 * <p>
 * This represents a data source implementation. A data source could be a database such as an RDBMS, OODBMS,
 * XML repository, flat file system, and so forth. A data source can also be another system
 * (legacy/mainframe), service (B2B service or credit card bureau), or some kind of repository (LDAP).
 * </p>
 * <p>
 * TransferObject
 * </p>
 * <p>
 * This represents a Transfer Object used as a data carrier. The DataAccessObject may use a Transfer Object to
 * return data to the client. The DataAccessObject may also receive the data from the client in a Transfer
 * Object to update the data in the data source.
 * </p>
 *
 * @author apete
 */
public abstract class AbstractBO<EB> extends Object implements Comparable<AbstractBO<EB>> {

    protected static final boolean DEBUG = false;

    private final EditingContext myEditingContext;
    private final EB myEntityBean;

    protected AbstractBO(final EditingContext editingContext, final EB entityBean) {

        super();

        myEditingContext = editingContext;
        myEntityBean = entityBean;

        this.awake(entityBean);
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AbstractBO)) {
            return false;
        }
        final AbstractBO<?> other = (AbstractBO<?>) obj;
        if (myEditingContext == null) {
            if (other.myEditingContext != null) {
                return false;
            }
        } else if (!myEditingContext.equals(other.myEditingContext)) {
            return false;
        }
        if (myEntityBean == null) {
            if (other.myEntityBean != null) {
                return false;
            }
        } else if (!myEntityBean.equals(other.myEntityBean)) {
            return false;
        }
        return true;
    }

    public final EditingContext getEditingContext() {
        return myEditingContext;
    }

    public final EB getEntityBean() {
        return myEntityBean;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((myEditingContext == null) ? 0 : myEditingContext.hashCode());
        result = (prime * result) + ((myEntityBean == null) ? 0 : myEntityBean.hashCode());
        return result;
    }

    public abstract String toDisplayString();

    public final String toKeyString() {
        return myEntityBean.toString();
    }

    @Override
    public final String toString() {
        return this.toKeyString() + " # " + this.toDisplayString() + " @ " + myEditingContext.getIdentifier();
    }

    protected abstract void appendAdditionalInserts(EditingContext editingContext);

    /**
     * awakeFromFetch and/or awakeFromInsertion
     */
    protected abstract void awake(EB entityBean);

    /**
     * <ul>
     * <li>Do not flush (FK) to-one relationsships. Load them lazily and set them explicitly, but never flush
     * them.</li>
     * <li>Flush to-many relationships by clearing the collection. Load lazily when/if the collection is
     * empty.</li>
     * <li>Store everything else in transient attributes and flush always/often.</li>
     * </ul>
     */
    protected abstract void flushCaches();

    protected abstract void prependAdditionalDeletes(EditingContext editingContext);

}
