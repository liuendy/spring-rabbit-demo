Spring RabbitMQ - Demo
====

# Para executar o código

É necessário ter um docker container do RabbitMQ. A seguir alguns comando úteis:

* Para *criar* um `docker container` do Rabbit execute o comando:

```
sudo  docker run -d --hostname localhost --name rabbit -p 15672:15672 -p 5672:5672  rabbitmq:3-management
```

* Para *parar* o docker container do rabbit execute:

```
sudo docker stop rabbit
```

* Para *iniciar* o docker container do rabbit execute:

```
sudo docker start rabbit
```

* Para entrar na linha de comando do docker container do RabbitMQ:

```
docker exec -ti rabbit bash
```

* Para *reiniciar* as filas de dentro do docker container do RabbitMQ:

```
rabbitmqctl stop_app && rabbitmqctl reset && rabbitmqctl start_app
```


# Acesso ao painel de administrações do RabbitMQ

Para acessar o painel de administração, quando o docker container do RabbitMQ estiver em execução, acesse o endereço http://localhost:15672/
