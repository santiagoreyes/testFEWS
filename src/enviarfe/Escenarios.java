/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package enviarfe;

import enviarfe.wscliente.EventoValidacion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 *
 * @author santi
 */
class Campo {

    @Attribute
    private String nombre;
    @Attribute
    private String valor;

    public Campo(String pnombre, String pvalor) {
        nombre = pnombre;
        valor = pvalor;
    }

}

class Escenario {

    @Element
    private String id;
    @Element
    private String descripcion;
    @Element
    private String esperado;
    @ElementList
    private List<Campo> campos;
    @Element(required=false)
    private Respuesta respuesta;

    public String getId(){
        return id;
    }
    public Escenario(String pid, List<Campo> pcampos 
            ) {
        id = pid;
        campos = pcampos;
        //respuestaMensaje = prespuestaMensaje;
    }
    
     public Escenario(String pid, String pDescripcion, String pEsperado,HashMap pCamposFila 
            ) {
        id = pid;
                descripcion = pDescripcion;
        esperado = pEsperado;

        List<Campo> lcampos = new ArrayList<Campo>();
        Iterator it = pCamposFila.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            lcampos.add(new Campo(pair.getKey().toString(), pair.getValue().toString()));
        }
        campos = lcampos;
        
        //respuestaMensaje = prespuestaMensaje;
    }
    
    public void setRespuestaMensaje(enviarfe.wscliente.RespuestaMensaje prespuestaMensaje){
        respuesta = new Respuesta(prespuestaMensaje);
    }

}

class Respuesta {
    @Element
    private String resultadoProceso;
    @Element(required=false)
    private String resultadoValidaciones;
    @Element(required=false) 
    private String idAutorizacion;
    @Element(required=false)
    private String idRechazo;
    @ElementList(required=false)
    private List<String> errores;
    
    public Respuesta(enviarfe.wscliente.RespuestaMensaje prespuestaMensaje){
        resultadoProceso = prespuestaMensaje.getRespuesta().getValue().getMensajeRespuesta().getValue();
        List<EventoValidacion> eventosValidacion 
                = prespuestaMensaje.getRespuesta().getValue().getResultadoProceso().getValue().getResultadoValidaciones().getValue().getEventosValidacion().getValue().getEventoValidacion();
        errores = new ArrayList<String>();
        for (int i=0; i<eventosValidacion.size();i++){
            errores.add(eventosValidacion.get(i).getError().getValue());
        }
        idAutorizacion = 
                prespuestaMensaje.getRespuesta().getValue().getResultadoProceso().getValue()
                .getAutorizacion().getValue() != null ?
                prespuestaMensaje.getRespuesta().getValue().getResultadoProceso().getValue()
                .getAutorizacion().getValue().getIdAutorizacion().getValue():
                null;
        idRechazo = 
                prespuestaMensaje.getRespuesta().getValue().getResultadoProceso().getValue()
                 .getRechazo().getValue() != null ?
                prespuestaMensaje.getRespuesta().getValue().getResultadoProceso().getValue()
                 .getRechazo().getValue().getIdRechazo().getValue():
                null;
    }
}

@Root
public class Escenarios {

    @ElementList
    private List<Escenario> escenarios;

    public Escenarios() {
        escenarios = (List) new ArrayList<Escenarios>();
    }

    public void add(Escenario escenario) {
        escenarios.add(escenario);
    }
    
    public void addResultadoByEscenarioId(String pId, 
            enviarfe.wscliente.RespuestaMensaje prespuestaMensaje){
        for (int i=0; i<escenarios.size();i++){
            if (((Escenario)escenarios.get(i)).getId() == pId){
                ((Escenario)escenarios.get(i)).setRespuestaMensaje(prespuestaMensaje);
            }
        }
    
    }
}
