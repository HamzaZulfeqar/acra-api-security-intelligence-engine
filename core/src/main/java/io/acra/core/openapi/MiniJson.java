package io.acra.core.openapi;
import java.util.*;
final class MiniJson {
    private final String s; private int i;
    private MiniJson(String s){this.s=s;}
    static Object parse(String s){MiniJson p=new MiniJson(s);Object v=p.value();p.ws();if(p.i!=s.length())throw new IllegalArgumentException("trailing JSON at "+p.i);return v;}
    private Object value(){ws();if(i>=s.length())throw new IllegalArgumentException("unexpected end");char c=s.charAt(i);return switch(c){case '{'->object();case '['->array();case '"'->string();case 't'->{literal("true");yield Boolean.TRUE;}case 'f'->{literal("false");yield Boolean.FALSE;}case 'n'->{literal("null");yield null;}default->number();};}
    private Map<String,Object> object(){i++;LinkedHashMap<String,Object> m=new LinkedHashMap<>();ws();if(peek('}')){i++;return m;}while(true){ws();String k=string();ws();expect(':');Object v=value();m.put(k,v);ws();if(peek('}')){i++;break;}expect(',');}return m;}
    private List<Object> array(){i++;ArrayList<Object> a=new ArrayList<>();ws();if(peek(']')){i++;return a;}while(true){a.add(value());ws();if(peek(']')){i++;break;}expect(',');}return a;}
    private String string(){expect('"');StringBuilder b=new StringBuilder();while(i<s.length()){char c=s.charAt(i++);if(c=='"')return b.toString();if(c=='\\'){if(i>=s.length())throw new IllegalArgumentException("bad escape");char e=s.charAt(i++);switch(e){case '"','\\','/'->b.append(e);case 'b'->b.append('\b');case 'f'->b.append('\f');case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'u'->{if(i+4>s.length())throw new IllegalArgumentException("bad unicode escape");b.append((char)Integer.parseInt(s.substring(i,i+4),16));i+=4;}default->throw new IllegalArgumentException("bad escape");}}else b.append(c);}throw new IllegalArgumentException("unterminated string");}
    private Number number(){int st=i;if(peek('-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;if(i<s.length()&&s.charAt(i)=='.'){i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(i<s.length()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}String n=s.substring(st,i);if(n.isBlank())throw new IllegalArgumentException("invalid JSON value at "+st);return n.contains(".")||n.contains("e")||n.contains("E")?Double.parseDouble(n):Long.parseLong(n);}
    private void literal(String x){if(!s.startsWith(x,i))throw new IllegalArgumentException("expected "+x);i+=x.length();}
    private void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
    private boolean peek(char c){return i<s.length()&&s.charAt(i)==c;}
    private void expect(char c){ws();if(!peek(c))throw new IllegalArgumentException("expected "+c+" at "+i);i++;}
}
