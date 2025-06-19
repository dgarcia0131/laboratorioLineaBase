# AceclapsFacade

## Descripci髇

Controlador (Facade) para la entidad Aceclaps.
Proporciona m茅todos para acceder y manipular informaci贸n relacionada con Aceclaps y operaciones asociadas.


**Author:** SmartTMT

## Campos

### log

**Tipo:** `org.apache.logging.log4j.Logger`

Logger para la clase AceclapsFacade.


### em

**Tipo:** `EntityManager`

EntityManager para operaciones de persistencia.


## M閠odos

### getEntityManager()

```java
protected EntityManager getEntityManager()
```

Obtiene el EntityManager asociado a esta fachada.


**Retorna:** el EntityManager.

### init()

```java
public void init()
```

Inicializa la fachada y registra el evento en el log.


### fun_max_aceclaps_acecenex_peri()

```java
public Aceclaps fun_max_aceclaps_acecenex_peri(String... param)
```

Ejecuta la funci贸n almacenada fun_max_aceclaps_acecenex_peri para obtener un objeto Aceclaps.

**Par醡etros:**
- `param`: Par谩metros requeridos por la funci贸n (sbCodiCiiu, sbPeriInic, sbPeriFina).

**Retorna:** Objeto Aceclaps obtenido o null si ocurre un error.

### cargaCampoSelectTipoActiEstaAcec()

```java
public List<CampoSelect> cargaCampoSelectTipoActiEstaAcec(String tipoSujeto, String sujeto, String sbPeriodo, String sbLapso)
```

Ejecuta la funci贸n almacenada fun_TipoActi_Carga_Campo_Sel para obtener una lista de CampoSelect.

- `tipoSujeto`: Tipo de sujeto.
- `sujeto`: Identificador del sujeto.
- `sbPeriodo`: Periodo.
- `sbLapso`: Lapso.

**Retorna:** Lista de CampoSelect obtenida o vac铆a si ocurre un error.

### getAcecLapsCiiu()

```java
public Aceclaps getAcecLapsCiiu(String periodo, String lapso, String tipoActividad, String actividad, String ciiu)
```

Ejecuta la funci贸n almacenada fun_AcecLaps_Ciiu_SysCursor para obtener un objeto Aceclaps seg煤n los par谩metros dados.

- `periodo`: Periodo.
- `lapso`: Lapso.
- `tipoActividad`: Tipo de actividad.
- `actividad`: Actividad.
- `ciiu`: C贸digo CIIU.

**Retorna:** Objeto Aceclaps obtenido o null si ocurre un error.

### getEm()

```java
public EntityManager getEm()
```

Obtiene el EntityManager utilizado por la fachada.


**Retorna:** el EntityManager.

### setEm()

```java
public void setEm(EntityManager em)
```

Establece el EntityManager utilizado por la fachada.

- `em`: EntityManager a establecer.


---

