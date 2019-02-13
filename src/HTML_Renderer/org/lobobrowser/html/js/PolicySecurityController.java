package org.lobobrowser.html.js;

import java.lang.ref.SoftReference;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.WeakHashMap;

import org.lobobrowser.util.NotImplementedYetException;
import org.lobobrowser.util.SecurityUtil;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.GeneratedClassLoader;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.SecurityController;

/**
 * This class is adapted from a class in Rhino by the same name.
 * 
 * We are defining it ourselves for two reasons:
 *   1. Since this class needs a lot of important permissions, using the one
 *      from Rhino would have required granting of all those permissions to Rhino
 *   2. We have added a clause to ensure codesource is always defined (non-null)
 *   
 */
public class PolicySecurityController extends SecurityController {
  private static final byte[] secureCallerImplBytecode = generateByteCodeForCaller();

  private static final Map<CodeSource, Map<ClassLoader, SoftReference<SecureCaller>>> callers = new WeakHashMap<CodeSource, Map<ClassLoader, SoftReference<SecureCaller>>>();

  @Override
  public Class<?> getStaticSecurityDomainClassInternal() {
    return CodeSource.class;
  }

  private static class Loader extends SecureClassLoader implements GeneratedClassLoader {
    private final CodeSource codeSource;

    Loader(final ClassLoader parent, final CodeSource codeSource) {
      super(parent);
      this.codeSource = codeSource;
    }

    @Override
    public Class<?> defineClass(final String name, final byte[] data) {
      return defineClass(name, data, 0, data.length, codeSource);
    }

    @Override
    public void linkClass(final Class<?> cl) {
      resolveClass(cl);
    }
  }

  @Override
  public GeneratedClassLoader createClassLoader(final ClassLoader parent, final Object securityDomain) {
    return SecurityUtil.doPrivileged(() -> new Loader(parent, (CodeSource) securityDomain));
  }

  @Override
  public Object getDynamicSecurityDomain(final Object securityDomain) {
    if (securityDomain == null) {
      throw new NotImplementedYetException("Security domain shouldn't be null when executing Javascript");
    }

    return securityDomain;
  }

  @Override
  public Object callWithDomain(final Object securityDomain, final Context cx,
      final Callable callable, final Scriptable scope, final Scriptable thisObj,
      final Object[] args) {
    System.out.println("Calling with domain: " + securityDomain);
    final ClassLoader classLoader = SecurityUtil.doPrivileged(() -> cx.getApplicationClassLoader());
    final CodeSource codeSource = (CodeSource) securityDomain;
    Map<ClassLoader, SoftReference<SecureCaller>> classLoaderMap;
    synchronized (callers) {
      classLoaderMap = callers.get(codeSource);
      if (classLoaderMap == null) {
        classLoaderMap = new WeakHashMap<ClassLoader, SoftReference<SecureCaller>>();
        callers.put(codeSource, classLoaderMap);
      }
    }
    SecureCaller caller;
    synchronized (classLoaderMap) {
      final SoftReference<SecureCaller> ref = classLoaderMap.get(classLoader);
      caller = ref != null ? ref.get() : null;

      if (caller == null) {
        try {
          // Run in doPrivileged as we'll be checked for "createClassLoader" runtime permission
          caller = (SecureCaller) AccessController.doPrivileged(
              new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                  final Loader loader = new Loader(classLoader, codeSource);
                  final Class<?> c = loader.defineClass( SecureCaller.class.getName() + "Impl", secureCallerImplBytecode);
                  return c.getConstructor().newInstance();
                }
              });
          classLoaderMap.put(classLoader, new SoftReference<SecureCaller>(caller));
        } catch (final PrivilegedActionException ex) {
          throw new UndeclaredThrowableException(ex.getCause());
        }
      }
    }
    return caller.call(callable, cx, scope, thisObj, args);
  }

  public abstract static class SecureCaller {
    public abstract Object call(Callable callable, Context cx, Scriptable scope, Scriptable thisObj, Object[] args);
  }

  // Creating a class this way allows us to specify the protection domain for the class
  // An alternative that doesn't work: defining a Proxy class. It fails because the protection domain is always fixed to the system level protection domain
  private static byte[] generateByteCodeForCaller() {
    final String secureCallerClassName = SecureCaller.class.getName();
    final ClassFileWriter cfw = new ClassFileWriter(secureCallerClassName + "Impl", secureCallerClassName, "<generated>");
    cfw.startMethod("<init>", "()V", ClassFileWriter.ACC_PUBLIC);
    cfw.addALoad(0);
    cfw.addInvoke(ByteCode.INVOKESPECIAL, secureCallerClassName, "<init>", "()V");
    cfw.add(ByteCode.RETURN);
    cfw.stopMethod((short) 1);

    final String callableCallSig = "Lorg/mozilla/javascript/Context;" +
        "Lorg/mozilla/javascript/Scriptable;" +
        "Lorg/mozilla/javascript/Scriptable;" +
        "[Ljava/lang/Object;)Ljava/lang/Object;";
    cfw.startMethod("call",
        "(Lorg/mozilla/javascript/Callable;" + callableCallSig,
        (short) (ClassFileWriter.ACC_PUBLIC | ClassFileWriter.ACC_FINAL));

    for (int i = 1; i < 6; ++i) {
      cfw.addALoad(i);
    }

    cfw.addInvoke(ByteCode.INVOKEINTERFACE, "org/mozilla/javascript/Callable", "call", "(" + callableCallSig);
    cfw.add(ByteCode.ARETURN);
    cfw.stopMethod((short) 6);
    return cfw.toByteArray();
  }
}