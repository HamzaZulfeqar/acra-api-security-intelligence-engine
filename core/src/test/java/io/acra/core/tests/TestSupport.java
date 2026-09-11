package io.acra.core.tests;

public final class TestSupport {
    private TestSupport() {}
    public static void assertTrue(boolean v,String m){if(!v)throw new AssertionError(m);}
    public static void assertFalse(boolean v,String m){assertTrue(!v,m);}
    public static void assertEquals(Object e,Object a,String m){if(e==null?a!=null:!e.equals(a))throw new AssertionError(m+" expected="+e+" actual="+a);}
    public static void assertContains(String hay,String needle,String m){if(hay==null||!hay.contains(needle))throw new AssertionError(m+" missing="+needle);}
    public static void assertNotContains(String hay,String needle,String m){if(hay!=null&&hay.contains(needle))throw new AssertionError(m+" leaked="+needle);}
    public static <T extends Throwable> void assertThrows(Class<T> type,Runnable r,String m){try{r.run();}catch(Throwable t){if(type.isInstance(t))return;throw new AssertionError(m+" wrong exception "+t,t);}throw new AssertionError(m+" expected exception "+type.getSimpleName());}
}
