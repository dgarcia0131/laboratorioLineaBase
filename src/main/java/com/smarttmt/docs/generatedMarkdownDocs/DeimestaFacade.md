# DeimestaFacade

## DescripciÛn

Controlador (Facade) para la entidad {@link Deimesta}.
Proporciona m√©todos para acceder y manipular informaci√≥n relacionada con Deimesta.


**Author:** SmartTMT

## Campos

### log

**Tipo:** `Logger`

Logger para la clase DeimestaFacade.


### em

**Tipo:** `EntityManager`

EntityManager para operaciones de persistencia sobre Deimesta.


## MÈtodos

### getEntityManager()

```java
protected EntityManager getEntityManager()
```

Obtiene el EntityManager asociado a esta fachada.


**Retorna:** el EntityManager utilizado para operaciones de persistencia.

### findDeimestaValiperi()

```java
public int findDeimestaValiperi(String periodo, String documento)
```

Busca la cantidad de registros en la tabla deimesta que coinciden con el periodo y documento dados.

**Par·metros:**
- `periodo`: Periodo a validar.
- `documento`: Documento (NIT) a validar.

**Retorna:** N√∫mero de registros encontrados. Devuelve 0 si ocurre un error.


---

