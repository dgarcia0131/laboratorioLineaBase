# EstadoFacadeLocal

## DescripciÛn

Interfaz local para operaciones CRUD sobre la entidad {@link Estado}.
Define los m√©todos para crear, editar, eliminar, buscar y contar entidades Estado.


**Author:** SmartTMT

## MÈtodos

### create()

```java
 abstract void create(Estado estado)
```

Crea un nuevo registro de {@link Estado}.

**Par·metros:**
- `estado`: entidad Estado a crear.

### edit()

```java
 abstract void edit(Estado estado)
```

Edita un registro existente de {@link Estado}.

- `estado`: entidad Estado a editar.

### remove()

```java
 abstract void remove(Estado estado)
```

Elimina un registro de {@link Estado}.

- `estado`: entidad Estado a eliminar.

### find()

```java
 abstract Estado find(Object id)
```

Busca un registro de {@link Estado} por su identificador.

- `id`: identificador de la entidad Estado.

**Retorna:** la entidad Estado encontrada, o null si no existe.

### findAll()

```java
 abstract List<Estado> findAll()
```

Obtiene todos los registros de {@link Estado}.


**Retorna:** lista de todas las entidades Estado.

### findRange()

```java
 abstract List<Estado> findRange(int[] range)
```

Obtiene un rango de registros de {@link Estado}.

- `range`: arreglo con dos posiciones: posici√≥n inicial y final del rango.

**Retorna:** lista de entidades Estado dentro del rango especificado.

### count()

```java
 abstract int count()
```

Cuenta el n√∫mero total de registros de {@link Estado}.


**Retorna:** n√∫mero total de entidades Estado.


---

