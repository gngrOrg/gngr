/*
    GNU LESSER GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
 */
/*
 * Created on Mar 20, 2005
 */
package org.lobobrowser.util;

//import java.util.logging.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author J. H. S.
 */
public class Bean {
  // private static final java.util.logging.Logger logger =
  // Logger.getLogger(Bean.class);
  private final Class<?> clazz;

  public Bean(final Class<?> clazz) {
    this.clazz = clazz;
  }

  private Map<String, PropertyDescriptor> propertyDescriptors = null;

  private void populateDescriptors(final Map<String, PropertyDescriptor> map, final Class<?> clazz) throws IntrospectionException {
    final BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
    final PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
    for (final PropertyDescriptor pd : pds) {
      map.put(pd.getName(), pd);
    }
    if (clazz.isInterface()) {
      final java.lang.reflect.Type[] interfaces = clazz.getGenericInterfaces();
      for (final Type interface1 : interfaces) {
        this.populateDescriptors(map, (Class<?>) interface1);
      }
    }
  }

  public PropertyDescriptor getPropertyDescriptor(final String propertyName) throws IntrospectionException {
    synchronized (this) {
      if (this.propertyDescriptors == null) {
        this.propertyDescriptors = new HashMap<>();
        this.populateDescriptors(this.propertyDescriptors, this.clazz);
      }
      return this.propertyDescriptors.get(propertyName);
    }
  }

  public Map<String, PropertyDescriptor> getPropertyDescriptorsMap() throws IntrospectionException {
    synchronized (this) {
      if (this.propertyDescriptors == null) {
        this.propertyDescriptors = new HashMap<>();
        this.populateDescriptors(this.propertyDescriptors, this.clazz);
      }
      return this.propertyDescriptors;
    }
  }

  public PropertyDescriptor[] getPropertyDescriptors() throws IntrospectionException {
    synchronized (this) {
      return this.getPropertyDescriptorsMap().values().toArray(new PropertyDescriptor[0]);
    }
  }

  public void setPropertyForFQN(final Object receiver, final String fullyQualifiedPropertyName, final Object value) throws Exception {
    final int idx = fullyQualifiedPropertyName.indexOf('.');
    if (idx == -1) {
      final PropertyDescriptor pd = this.getPropertyDescriptor(fullyQualifiedPropertyName);
      if (pd == null) {
        throw new IllegalStateException("Property '" + fullyQualifiedPropertyName + "' unknown");
      }
      final Method method = pd.getWriteMethod();
      if (method == null) {
        throw new IllegalStateException("Property '" + fullyQualifiedPropertyName + "' not settable");
      }
      final Object actualValue = convertValue(value, pd.getPropertyType());
      method.invoke(receiver, new Object[] { actualValue });
    } else {
      final String prefix = fullyQualifiedPropertyName.substring(0, idx);
      final PropertyDescriptor pinfo = this.getPropertyDescriptor(prefix);
      if (pinfo == null) {
        throw new IllegalStateException("Property '" + prefix + "' unknown");
      }
      final Method readMethod = pinfo.getReadMethod();
      if (readMethod == null) {
        throw new IllegalStateException("Property '" + prefix + "' not readable");
      }
      final Object newReceiver = readMethod.invoke(receiver, new Object[0]);
      // Class newClass = pinfo.getPropertyType();
      final String nameRest = fullyQualifiedPropertyName.substring(idx + 1);
      this.setPropertyForFQN(newReceiver, nameRest, value);
    }
  }

  private static Object convertValue(Object value, final Class<?> targetType) {
    final boolean targetString = targetType.isAssignableFrom(String.class);
    if ((value instanceof String) && targetString) {
      // ignore
    } else if (targetString) {
      value = String.valueOf(value);
    } else if (!(value instanceof Byte) && ((targetType == Byte.class) || (targetType == byte.class))) {
      value = Byte.valueOf(String.valueOf(value));
    } else if (!(value instanceof Boolean) && ((targetType == Boolean.class) || (targetType == boolean.class))) {
      value = Boolean.valueOf(String.valueOf(value));
    } else if (!(value instanceof Short) && ((targetType == Short.class) || (targetType == short.class))) {
      value = Short.valueOf(String.valueOf(value));
    } else if (!(value instanceof Integer) && ((targetType == Integer.class) || (targetType == int.class))) {
      value = Integer.valueOf(String.valueOf(value));
    } else if (!(value instanceof Long) && ((targetType == Long.class) || (targetType == long.class))) {
      value = Long.valueOf(String.valueOf(value));
    }
    return value;
  }

}
