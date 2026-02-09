package com.wisetech.rras.calculationengine.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.calculation}")
    private String calculationExchange;

    @Value("${rabbitmq.queue.calculation.rwa}")
    private String rwaQueue;

    @Value("${rabbitmq.queue.calculation.car}")
    private String carQueue;

    @Value("${rabbitmq.queue.calculation.lcr}")
    private String lcrQueue;

    @Value("${rabbitmq.queue.calculation.npl}")
    private String nplQueue;

    @Value("${rabbitmq.queue.calculation.ecl}")
    private String eclQueue;

    @Value("${rabbitmq.queue.snapshot}")
    private String snapshotQueue;

    @Value("${rabbitmq.queue.notification}")
    private String notificationQueue;

    // Exchange
    @Bean
    public TopicExchange calculationExchange() {
        return new TopicExchange(calculationExchange);
    }

    // Queues
    @Bean
    public Queue rwaQueue() {
        return QueueBuilder.durable(rwaQueue)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue carQueue() {
        return QueueBuilder.durable(carQueue)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue lcrQueue() {
        return QueueBuilder.durable(lcrQueue)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue nplQueue() {
        return QueueBuilder.durable(nplQueue)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue eclQueue() {
        return QueueBuilder.durable(eclQueue)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue snapshotQueue() {
        return QueueBuilder.durable(snapshotQueue)
                .withArgument("x-message-ttl", 3600000)
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(notificationQueue)
                .withArgument("x-message-ttl", 86400000) // 24 hours
                .build();
    }

    // Bindings
    @Bean
    public Binding rwaBinding() {
        return BindingBuilder.bind(rwaQueue())
                .to(calculationExchange())
                .with("calculation.rwa");
    }

    @Bean
    public Binding carBinding() {
        return BindingBuilder.bind(carQueue())
                .to(calculationExchange())
                .with("calculation.car");
    }

    @Bean
    public Binding lcrBinding() {
        return BindingBuilder.bind(lcrQueue())
                .to(calculationExchange())
                .with("calculation.lcr");
    }

    @Bean
    public Binding nplBinding() {
        return BindingBuilder.bind(nplQueue())
                .to(calculationExchange())
                .with("calculation.npl");
    }

    @Bean
    public Binding eclBinding() {
        return BindingBuilder.bind(eclQueue())
                .to(calculationExchange())
                .with("calculation.ecl");
    }

    @Bean
    public Binding snapshotBinding() {
        return BindingBuilder.bind(snapshotQueue())
                .to(calculationExchange())
                .with("snapshot.#");
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(calculationExchange())
                .with("notification.#");
    }

    // Message converter for JSON
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}