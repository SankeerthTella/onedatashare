package org.onedatashare.server.service;

import org.onedatashare.server.model.core.Mail;
import org.onedatashare.server.model.core.User;
import org.onedatashare.server.model.core.UserDetails;
import org.onedatashare.server.model.jobaction.JobRequest;
import org.onedatashare.server.repository.MailRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import org.onedatashare.server.repository.UserRepository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Service which backs Admin controller
 */
@Service
public class AdminService {
    private final UserRepository userRepository;
    private final MailRepository mailRepository;

    private static Pageable generatePageFromRequest(JobRequest request){
        Sort.Direction direction = request.sortOrder.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        request.pageNo = request.pageNo - 1 < 0 ? 0 : request.pageNo - 1;
        Pageable page = PageRequest.of(request.pageNo, request.pageSize, Sort.by(direction, request.sortBy));
        return page;
    }

    public AdminService(UserRepository userRepository, MailRepository mailRepository) {
        this.userRepository = userRepository;
        this.mailRepository = mailRepository;
    }

    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Flux<User> getAllAdmins() {
        return userRepository.getAllAdminIds();
    }

    public Mono<Mail> saveMail(Mail mail) {
        if (mail.getUuid() == null) {
            mail.uuid();
        }
        return mailRepository.save(mail);
    }

    public Mono<Mail> deleteMail(String id) {
        return mailRepository.findById(UUID.fromString(id)).map(mail -> {
            mail.setStatus("deleted");
            mailRepository.save(mail).subscribe();
            return mail;
        });
    }

    public Flux<Mail> getAllMails() {
        return mailRepository.findAll();
    }

    public Flux<Mail> getTrashMails() {
        return mailRepository.findAllDeleted();
    }

    public Mono<UserDetails> getAdminsPaged(JobRequest jobRequest){
        Pageable pageable = generatePageFromRequest(jobRequest);
        Mono<List<User>> admins = userRepository.findAllAdministrators(pageable).collectList();
        Mono<Long> adminCount = userRepository.countAdministrators();

        return admins.zipWith(adminCount, UserDetails::new);
    }

    public Mono<UserDetails> getUsersPaged(JobRequest jobRequest) {
        Pageable pageable = generatePageFromRequest(jobRequest);
        Mono<List<User>> users = userRepository.findAllUsers(pageable).collectList();
        Mono<Long> userCount = userRepository.countAdministrators();

        return users.zipWith(userCount, UserDetails::new);
    }
}