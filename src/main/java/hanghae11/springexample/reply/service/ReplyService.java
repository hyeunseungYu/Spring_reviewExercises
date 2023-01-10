package hanghae11.springexample.reply.service;

import hanghae11.springexample.reply.dto.ReplyRequestDto;
import hanghae11.springexample.reply.dto.ReplyRequestMsgDto;
import hanghae11.springexample.entity.FreeBoard;
import hanghae11.springexample.entity.Member;
import hanghae11.springexample.entity.MemberRoleEnum;
import hanghae11.springexample.entity.Reply;
import hanghae11.springexample.jwt.JwtUtil;
import hanghae11.springexample.repository.FreeBoardRepository;
import hanghae11.springexample.member.repository.MemberRepository;
import hanghae11.springexample.reply.repository.ReplyRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class ReplyService {
    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final ReplyRepository replyRepository;
    private final FreeBoardRepository freeBoardRepository;

    public ReplyRequestMsgDto createReply(Long id, ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        // Request에서 Token 가져오기
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getUserInfoFromToken(token);
            }else {
                return new ReplyRequestMsgDto("토큰이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value());
            }

            //댓글 남기는 사용자 확인
            Member member = memberRepository.findByUsername(claims.getSubject());

            //param에 해당하는 게시글 찾기
            FreeBoard entity = freeBoardRepository.findById(id).orElseThrow(
                    () -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다.")
            );

            //builder로 entity생성
            Reply reply = Reply.builder()
                    .contents(replyRequestDto.getContents())
                    .username(claims.getSubject())
                    .freeboard(entity)
                    .build();


            replyRepository.save(reply);
            return new ReplyRequestMsgDto("댓글을 등록하였습니다.", HttpStatus.OK.value());
        }else {
            return new ReplyRequestMsgDto("토큰이 유효하지 않습니다.",HttpStatus.BAD_REQUEST.value());
        }



    }

    @Transactional
    public ReplyRequestMsgDto editReply(Long id, ReplyRequestDto replyRequestDto, HttpServletRequest request) {
        // Request에서 Token 가져오기
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getUserInfoFromToken(token);
            } else {
                return new ReplyRequestMsgDto("토큰이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value());
            }

            //토큰에 들어있는 username으로 member 찾기
            Member member = memberRepository.findByUsername(claims.getSubject());

            //param에 해당하는 댓글 찾기
            Reply reply = replyRepository.findById(id).orElseThrow(
                    () -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다.")
            );

            //param에 해당하는 게시글 찾기
            FreeBoard freeBoard = freeBoardRepository.findById(id).orElseThrow(
                    () -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다.")
            );

            //사용자가 admin이면 바로 수정
            if (member.getRole().equals(MemberRoleEnum.ADMIN)) {
                reply.update(replyRequestDto, freeBoard);
                return new ReplyRequestMsgDto("수정하였습니다.", HttpStatus.OK.value());
            }

            //토큰에 들어있는 이름과 게시글의 이름이 다르면 토큰을 가진 사용자가 작성한 글이 아님
            if (!reply.getUsername().equals(member.getUsername())){
                return new ReplyRequestMsgDto("본인이 작성한 글만 수정할 수 있습니다.", HttpStatus.BAD_REQUEST.value());
            }

            reply.update(replyRequestDto, freeBoard);
            return new ReplyRequestMsgDto("수정하였습니다.", HttpStatus.OK.value());

        }else {
            return new ReplyRequestMsgDto("토큰이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value());
        }

    }


    public ReplyRequestMsgDto deleteReply(Long id, HttpServletRequest request) {
        // Request에서 Token 가져오기
        String token = jwtUtil.resolveToken(request);
        Claims claims;

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                claims = jwtUtil.getUserInfoFromToken(token);
            } else {
                return new ReplyRequestMsgDto("토큰이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value());
            }

            Member member = memberRepository.findByUsername(claims.getSubject());

            Reply reply = replyRepository.findById(id).orElseThrow(
                    () -> new IllegalArgumentException("해당 댓글이 존재하지 않습니다.")
            );

            //사용자가 admin이면 바로 삭제
            if (member.getRole().equals(MemberRoleEnum.ADMIN)) {
                replyRepository.deleteById(id);
                return new ReplyRequestMsgDto("삭제하였습니다.", HttpStatus.OK.value());
            }

            if (!reply.getUsername().equals(member.getUsername())) {
                return new ReplyRequestMsgDto("본인이 작성한 댓글만 삭제할 수 있습니다.", HttpStatus.BAD_REQUEST.value());
            }

            replyRepository.deleteById(id);
            return new ReplyRequestMsgDto("삭제하였습니다.", HttpStatus.OK.value());
        } else {
            return new ReplyRequestMsgDto("토큰이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value());
        }
    }
}