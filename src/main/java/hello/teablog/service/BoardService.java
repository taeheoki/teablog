package hello.teablog.service;

import hello.teablog.core.exception.ssr.Exception400;
import hello.teablog.core.util.MyParseUtil;
import hello.teablog.dto.board.BoardRequest;
import hello.teablog.model.board.Board;
import hello.teablog.model.board.BoardQueryRepository;
import hello.teablog.model.board.BoardRepository;
import hello.teablog.model.user.User;
import hello.teablog.model.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class BoardService {


    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final BoardQueryRepository boardQueryRepository;

    @Transactional
    public void 글쓰기(BoardRequest.SaveInDTO saveInDTO, Long userId) {
        try {
            // 1. 유저 존재 확인
            User userPS = userRepository.findById(userId).orElseThrow(
                    () -> new RuntimeException("유저를 찾을 수 없습니다.")
            );

            // 2. 썸네일 만들기
            String thumbnail = MyParseUtil.getThumbnail(saveInDTO.getContent());

            // 3. 게시글 쓰기
            boardRepository.save(saveInDTO.toEntity(userPS, thumbnail));
        } catch (Exception e) {
            throw new RuntimeException("글쓰기 실패 : " + e.getMessage());
        }
    }

    @Transactional(readOnly = true) // 변경감지 하지마, 고립성(REPEATABLE READ)
    public Page<Board> 글목록보기(int page) { // CSR은 DTO로 변경해서 돌려줘야 함.
        // 1. 모든 전략은 Lazy : 이유는 필요할 때만 가져오려고
        // 2. 필요할때는 직접 fetch join을 사용해라
        return boardQueryRepository.findAll(page);
    }

    public Board 게시글상세보기(Long id) {
        Board boardPS = boardRepository.findByIdFetchUser(id).orElseThrow(
                ()-> new Exception400("id", "게시글 아이디를 찾을 수 없습니다")
        );
        // 1. Lazy Loading 하는 것 보다 join fetch 하는 것이 좋다.
        // 2. 왜 Lazy를 쓰냐면, 쓸데 없는 조인 쿼리를 줄이기 위해서이다.
        // 3. 사실 @ManyToOne은 Eager 전략을 쓰는 것이 좋다.
        // boardPS.getUser().getUsername();
        return boardPS;
    }
}
