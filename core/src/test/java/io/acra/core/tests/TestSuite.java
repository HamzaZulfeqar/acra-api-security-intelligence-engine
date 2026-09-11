package io.acra.core.tests;

public final class TestSuite {
    public static void main(String[] args){int total=0; long start=System.nanoTime();
        total+=run("HTTP",HttpModelTests::run); total+=run("URI",UriTests::run); total+=run("CONTEXT",ContextTests::run); total+=run("GRAPH",GraphTests::run); total+=run("SERIALIZATION_SECURITY",SerializationSecurityTests::run); total+=run("RESOLUTION_CONTRACT",ResolutionContractTests::run); total+=run("NEGATIVE_SECURITY",NegativeSecurityTests::run); total+=run("BOLA_ASSESSMENT",BolaAssessmentTests::run); total+=run("BFLA_ASSESSMENT",BflaAssessmentTests::run);
        long ms=(System.nanoTime()-start)/1_000_000; System.out.println("PASS total="+total+" durationMs="+ms);
    }
    private static int run(String name,Counter c){try{int n=c.run();System.out.println("PASS "+name+" tests="+n);return n;}catch(Throwable t){System.err.println("FAIL "+name+" "+t);t.printStackTrace();System.exit(1);return 0;}}
    @FunctionalInterface interface Counter{int run();}
}
