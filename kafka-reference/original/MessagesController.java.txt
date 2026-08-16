package com.jurisai.controller;

import com.jurisai.dao.repository.ConversationsRepo;
import com.jurisai.dto.request.MessageRequestDTO;
import com.jurisai.dto.response.*;
import com.jurisai.entity.Conversations;
import com.jurisai.entity.Messages;
import com.jurisai.entity.pk.MessagePK;
import com.jurisai.mapper.MessageMapper;
import com.jurisai.service.ConversationsService;
import com.jurisai.service.MessagesService;
import com.jurisai.util.ConversationConstant;
import com.jurisai.util.kafka.KafkaMessageProducer;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/v1/message-service/{accountId}")
@RequiredArgsConstructor
public class MessagesController {

    private final MessagesService messagesService;
    private final KafkaMessageProducer kafkaMessageProducer;
    private final ConversationsService conversationsService;

    @PostMapping("/conversations/{conversationId}/messages")
    public CustomResponse<SingleMessageResponseDTO> postMessage(@RequestBody MessageRequestDTO body,
                                                                @PathVariable("accountId") Long accountId,
                                                                @PathVariable("conversationId") Long conversationId
    ){
        // check conversationId
        Conversations conversations = conversationsService.findByPimaryKey(accountId, conversationId);
        if (conversations == null){
            return new CustomResponse<>(null, "Not found conversation by ID!");
        }

        // gen messagePK
        MessagePK messagePK = messagesService.genMessagePK(accountId, conversationId);

        // create new message
        Messages messages = MessageMapper.toEntity(body);
        messages.setId(messagePK);

        // create child messageId (update later: save to kafka)
        long timeStamp = new Date().getTime();
        MessagePK childMessagePK = messagesService.genMessagePK(accountId, conversationId);
        Messages childMessage = Messages.builder()
                .parentMessagesId(messagePK.getMessageId())
                .id(childMessagePK)
                .sender(0)
                .status(1)
                .createdAt(timeStamp)
                .updatedAt(timeStamp)
                .build();




        // save message
        messagesService.addOrUpdate(childMessage);
        messagesService.addOrUpdate(messages);

        // send kafka
        JSONObject object = sendMessage(messages, childMessagePK);
        kafkaMessageProducer.sendMessage("questions_topic", object.toString());

        // update status conversation
        conversations.setStatus(ConversationConstant.ConversationStatus.IN_PROCESS.getValue());
        conversations.setUpdatedAt(timeStamp);
        conversationsService.addOrUpdate(conversations);

        // build Message response
        SingleMessageResponseDTO res = MessageMapper.toSingleResponse(messages, childMessagePK.getMessageId());

        return new CustomResponse<>(res, "post success");
    }

    @GetMapping("/conversations/{conversationId}/messages/{messageId}")
    public CustomResponse<SingleMessageResponseDTO> getMessage(@PathVariable("accountId") Long accountId,
                                                               @PathVariable("conversationId") Long conversationId,
                                                               @PathVariable("messageId") Long messageId
    ){
        // check accountId

        // check messageId
        Messages messages = messagesService.findByPimaryKey(accountId, conversationId, messageId);
        // save Message
        if (messages == null){
            return new CustomResponse<>(null, "Not found message by Id!");
        }

        // build Message response
        SingleMessageResponseDTO res = MessageMapper.toSingleResponse(messages);
        return new CustomResponse<>(res, "Get message by id success!");
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public CustomResponse<ListMessagesResponseDTO> getListMessage(@PathVariable("accountId") Long accountId,
                                                                  @PathVariable("conversationId") Long conversationId,
                                                                  @RequestParam(required = false, defaultValue = "20") Integer size,
                                                                  @RequestParam(required = false, defaultValue = "0") Integer page,
                                                                  @RequestParam(required = false, defaultValue = "id.messageId") String sortBy
    ){

        // check accountId

        // check messageId
        Page<Messages> messages = messagesService.findListMessage(size, page, sortBy, accountId, conversationId);
        // save Message
        if (messages == null){
            return new CustomResponse<>(null, "Not found message by Id!");
        }


        // build Message response
        ListMessagesResponseDTO res = MessageMapper.toListResponse(messages);
        return new CustomResponse<>(res, "Get message by id success!");
    }

    @PatchMapping("/conversations/{conversationId}/messages/{messageId}")
    public CustomResponse<SingleMessageResponseDTO> updateMessage(@RequestBody MessageRequestDTO body,
                                                            @PathVariable("accountId") Long accountId,
                                                            @PathVariable("conversationId") Long conversationId,
                                                            @PathVariable("messageId") Long messageId
    ){
        // check accountId

        // get message by id
        Messages message = messagesService.findByPimaryKey(accountId, conversationId, messageId);
        // save Message
        if (message == null){
            // should create throw exception
            return new CustomResponse<>(null, "Not found message by Id!");
        }

        if(!body.getContent().equals(message.getContent())) {
            message.setContent(body.getContent());
        }

        messagesService.addOrUpdate(message);

        // build Message response
        SingleMessageResponseDTO res = MessageMapper.toSingleResponse(message);
        return new CustomResponse<>(res, "Get message by id success!");
    }

    @PutMapping("/conversations/{conversationId}/messages/{messageId}/feedback/{feedbackId}")
    public CustomResponse<MessageResponseDTO> updateReview(@PathVariable("accountId") Long accountId,
                                                           @PathVariable("conversationId") Long conversationId,
                                                           @PathVariable("messageId") Long messageId,
                                                           @PathVariable("feedbackId") Integer feedbackId
    ){
//        // check accountId
//
//        // get message by id
//        Messages message = messagesService.findByPimaryKey(accountId, conversationId, messageId);
//        // save Message
//        if (message == null){
//            // should create throw exception
//            return new CustomResponse<>(null, "Not found message by Id!");
//        }
//
//        if(feedbackId != MessageConstant.TypeFeedback.getValue()){
//            // should create throw exception
//        }
//        message.setFeedback(feedbackId);
//
//        // build Message response
//        MessageResponseDTO res = messageMapper.toDto(message);
//        return new CustomResponse<>(res, "Get message by id success!");
        return null;
    }

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    public CustomResponse<MessageResponseDTO> deleteMessage(@PathVariable("accountId") Long accountId,
                                                           @PathVariable("conversationId") Long conversationId,
                                                           @PathVariable("messageId") Long messageId
    ){
//        // check accountId
//
//        // get message by id
//        Messages message = messagesService.findByPimaryKey(accountId, conversationId, messageId);
//        // save Message
//        if (message == null){
//            // should create throw exception
//            return new CustomResponse<>(null, "Not found message by Id!");
//        }
//
//        if(feedbackId != MessageConstant.TypeFeedback.getValue()){
//            // should create throw exception
//        }
//        message.setFeedback(feedbackId);
//
//        // build Message response
//        MessageResponseDTO res = messageMapper.toDto(message);
//        return new CustomResponse<>(res, "Get message by id success!");
        return null;
    }


    private JSONObject sendMessage(Messages messages, MessagePK messagePK) {

        JSONObject object = new JSONObject();
        object.put("account_id", messages.getId().getAccountId());
        object.put("conversations_id", messages.getId().getConversationsId());
        object.put("message_id", messagePK.getMessageId());
        object.put("model_chat", messages.getModelChat());
        object.put("content", messages.getContent());

        // test
//        JSONArray citations = new JSONArray();
//        JSONObject citation = new JSONObject();
//        citation.put("law_name", "test");
//        citation.put("chapter", "test");
//        citation.put("section", "test");
//        citation.put("article", "test");
//        citations.put(citation);
//        object.put("citations",citations);
        return object;
    }

}
