
# 1-Vista entrenamiento:
- Al abrir la rutina para empezar, los ejercicios deben empezar todos colapsados.
- Cuando un ejercicio, tenga todas las series completadas, con el fin del cronometro, el ejercicio
debe colapsarse automaticamente marcado como completado.
- Cuando haga tap para editar algun valor de kg, reps, o pausa, si el valor está en 0 y comienzo a 
añadir valores, ejemplo 0 -> 12, el 0 debe borrarse y escribirse 12, evitando 0 a la izquierda. 
ejemplo : 0 -> 012. 
Solo permitir el 0, si muevo el cursor a su izquierda. ejemplo: 0 -> 10
- Deslizar un ejercicio colapsado a la izquierda mostrará un botón de sustituir ejericio, esto nos 
permitirá ejegir de nuevo un ejercicio para sustituirlo por otro nuevo que quedará guardado en la 
rutina de forma inmediata y permanente, como la edicion de series por ejercicio.

# 2-Inicio rutina: 
- ¿Que ocurre actualmente si está ejecutándose una rutina y salgo de la rutina sin guardar ni pausar?
Debe quedar constancia que la rutina está en marcha y debe poder abrirse para retomar donde se ha 
dejado.

# 3-Crear rutina: 
- Esta vista, al abrir, debe mostrar la primera pestaña con nombre de rutina, duracion etc, colapsado
- El dato de duración de la rutina, debe aparecer vacío. A medida que se añadan ejercicios, se hará 
una suma de todos los tiempos de descanso + 30 seg por serie y pondrá que la duración es aprox. Una vez
completada la sesión de entrenamiento, éste dato se actualizará con el tiempo real realizado ese día.
Cada día que la rutina se complete, ese valor quedará guardado como el valor final de la rutina. Mas 
Adelante lo quiero utilizar para la estadística de progreso.

# 4-Teclados:
- A la hora de introducir numeros en las rutinas para los kg, las reps o el descanso, debemos tener 
teclado numerico al estilo de la app. pero sin tener letras alguna, el estilo de una calculadora.

# 5-Editar rutina:
- La edición de los parámetros de cada serie deben modificarse sin abrir un modal con los 3 datos 
sino mas bien ponese el input seleccionado en parpadeo y levantar el teclado de edición para cambiar 
el valor del mismo. Una vez pulsado ok, el valor queda modificado.

# 6-Nombres en ingles:
- Toca realizar una revision del codigo para comprobar si hay variables, funciones etc en español y
de ser así, cambiar el nombre a ingles, sin romper nada, comprobar dónde se utiliza cada variable 
funcion etc para que los nombres coincidan. 

# 7-Reparacion codigo poco legible.
- Toca hacer una revisión de logica de cogido y funciones, comprobar que  el codigo sea legible para 
un aprendiz y no utilizar funciones con llamadas a variables sin sentido, ejemplo con nombre a, b, c
porque esto dificulta seguir el codigo para un humano.Hay que utilizar nombres relacionados con lo 
que va a crear o realizar.

# Comprobar los tests.

- Debemos realizar un seguimiento de los test y los checks que deben pasar en cada spec hasta ahora.
Comprobar que no se están solapando, que no caen en contradicciones, asegurar que cada vez que hacemos
un cambio, un spec nuevo etc, el código anterior sigue pasando los test anteriores ( si creamos un 
boton para borrar algo, que ese botón sigue funcionando).