package com.example.v_o_server.domain.question;

import com.example.v_o_server.domain.group.entity.GroupThemeCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 기본 질문 seed. 테마별로 질문을 넣고 {@code question_theme_maps}에 매핑한다.
 *
 * <p>{@link com.example.v_o_server.domain.group.GroupThemeSeeder}가 먼저 테마를 만들어 두어야
 * 하므로 {@link org.springframework.core.annotation.Order} 없이도 동작하도록,
 * 테마를 찾지 못하면 해당 질문만 건너뛴다.</p>
 *
 * <p>질문을 추가하려면 {@link #SEEDS}에 줄을 추가하고 배포하면 된다. 이미 들어간 질문은
 * {@link QuestionSeedWriter}가 content로 확인해 건너뛰므로 재기동해도 중복되지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSeeder implements ApplicationRunner {

    /** 모든 질문의 촬영 제한 시간(10초). 질문별로 달라지면 {@link QuestionSeed}로 옮길 것. */
    private static final int ANSWER_TIME_LIMIT_MS = 10_000;

    private record QuestionSeed(GroupThemeCode theme, String content) {
    }

    private static final List<QuestionSeed> SEEDS = List.of(
            // 친구
            new QuestionSeed(GroupThemeCode.FRIEND, "지금 눈앞에 보이는 게 뭔지 10초 안에 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "오늘 입은 옷 상체 착장 스타일을 살짝 보여주세요!"),
            new QuestionSeed(GroupThemeCode.FRIEND, "그룹에서 연락 제일 안 보는 사람에게 한마디 한다면?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "솔직히 이 그룹에서 본인의 외모 순위는 몇 위 같나요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "주변 물건 하나로 킹받는 ASMR 소리를 내볼까요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "지금 로또 1등 당첨된 사람처럼 리액션을 해볼까요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "현재 내 인스타 돋보기 알고리즘은 무엇으로 가득 차있나요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "카메라 가까이 얼굴을 밀착해서 킹받는 표정을 지어보세요!"),
            new QuestionSeed(GroupThemeCode.FRIEND, "최근에 산 물건이 뭔지 왜 샀는지 얘기해줄래요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "목소리 없이 표정만으로 배고픈 걸 표현해 주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "세상에서 제일 억울한 표정을 10초간 지어볼까요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "10초 동안 눈을 한 번도 안 감고 정면을 바라볼 수 있나요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "10초 동안 손가락 하트, 볼 하트를 연속으로 날려주세요!"),
            new QuestionSeed(GroupThemeCode.FRIEND, "지금 주변 상태가 어떤지 한 단어로 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "가방이나 주머니 속 제일 쓸데없는 물건은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "스크린타임이 제일 오래 찍힌 앱이 뭔지 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "10초 안에 '바'로 시작하는 단어 7개를 말씀하실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "평생 안 씻기 vs 평생 안 자기, 3초 만에 골라볼까요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "10초 안에 본인 장점 3개를 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.FRIEND, "초성 'ㄱㄱ'으로 시작하는 단어 5개를 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "최근 3일 동안 드신 저녁 메뉴를 순서대로 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.FRIEND, "본인의 MBTI와 그 성격의 핵심 특징을 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.FRIEND, "지금 당장 제일 먹고 싶은 메뉴는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "친구를 처음 만났을 때 첫인상이 어땠는지 기억나시나요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "친구한테 말은 안 했지만 서운했던 순간이 있으신가요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "친구가 가진 능력 중 제일 탐나는 거 하나는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "본인 인생에서 가장 지우고 싶은 흑역사는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "솔직히 본인 패션 센스는 10점 만점에 몇 점인가요?"),
            new QuestionSeed(GroupThemeCode.FRIEND, "이 그룹 친구와 꼭 같이 해보고 싶은 버킷리스트는 무엇인가요?"),

            // 연인
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 어디가 제일 좋은지 10초 안에 3가지 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "다음 데이트 때 당장 가고 싶은 곳은 어디인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "처음 만났던 날 애인을 보고 무슨 생각이 드셨나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "카메라를 향해 필살기 애교를 발사해 주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "10초 동안 눈 안 피하고 아이컨택이 가능하신가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "달콤한 표정으로 연속 뽀뽀 3번을 날려주세요!"),
            new QuestionSeed(GroupThemeCode.COUPLE, "마이크에 가까이 대고 '사랑해'라고 속삭여주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "손가락 하트, 볼 하트를 3연속으로 날려볼까요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 제일 좋아할 만한 표정을 지어보세요!"),
            new QuestionSeed(GroupThemeCode.COUPLE, "드라마 주인공처럼 애인에게 고백해본다면?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "본인만의 제일 예쁜 심쿵 눈웃음을 보여주세요!"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인 이름 세 글자로 삼행시를 지어주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이랑 같이 찍은 사진 중 최애 사진은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이랑 나눈 카톡 중 제일 설렜던 메시지는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "오늘 입은 옷 중 애인이 좋아할 만한 포인트는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이랑 주고받은 메시지 중 제일 웃긴 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "연락처에 애인 이름을 뭐라고 저장하셨는지 외쳐볼까요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "최근 같이 먹은 음식 중 제일 맛있었던 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "휴대폰에 몰래 소장 중인 애인 엽사는 어떤 사진인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "둘이 갔던 데이트 장소 중 최고의 인생 장소는 어디인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 써준 편지나 메모 중 제일 기억나는 문구는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 제일 좋아하는 음식 3가지를 맞히실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "우리 처음 만난 연도와 월, 일을 외쳐주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인 생일이랑 휴대폰 번호 뒷자리 4개를 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.COUPLE, "본인이 연애할 때 제일 싫어하는 행동은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 제일 예뻐 보이거나 멋져 보이는 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인의 신발 사이즈랑 상의 옷 사이즈를 알고 계시나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 제일 자주 마시는 카페 음료 메뉴는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "우리 둘이 자주 쓰는 전용 애칭 3개는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "10년 뒤 우리 두 사람의 모습을 한 문장으로 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.COUPLE, "연애하면서 제일 서운했던 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이랑 진짜 오래 함께하고 싶다고 느낀 순간이 있으신가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "솔직히 말 안 하고 속으로 질투한 적이 있으신가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "우리 싸웠을 때 최고로 빠른 화해 방법은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인을 만나고 나서 제일 긍정적으로 바뀐 점은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인을 한 단어로 표현하면 어떤 단어 같나요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "데이트하면서 시간이 멈췄으면 했던 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 해준 말 중 제일 힘이 되었던 한마디는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인 습관 중에서 제일 사랑스러워하는 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인을 향한 사랑 온도를 숫자로 말씀해 주신다면 몇 도인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "애인이 곁에 있어서 제일 고마웠던 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.COUPLE, "10초 동안 오늘 애인한테 하고 싶은 진심 한마디를 해주세요!"),

            // 가족
            new QuestionSeed(GroupThemeCode.FAMILY, "오늘 하루 동안 제일 기분 좋았던 일은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "오늘 저녁 메뉴나 제일 먹고 싶은 집밥은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족끼리 제일 고마웠던 순간을 10초 안에 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "환한 미소를 지으며 10초 동안 하트를 크게 날려주세요!"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족들을 향해서 필살기 애교를 발사해 주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "'우리 가족 사랑해'라고 크게 3번 외쳐볼까요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "손가락 하트부터 대형 하트까지 3연속 포즈를 해보세요!"),
            new QuestionSeed(GroupThemeCode.FAMILY, "신나는 노래 한 구절을 불러주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "카메라를 향해 '파이팅'을 외치며 힘을 나눠주세요!"),
            new QuestionSeed(GroupThemeCode.FAMILY, "집에서 제일 자주 듣는 잔소리 말투를 흉내 내볼까요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족들을 향해 10초 동안 폭풍 칭찬을 해볼까요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족들과 찍은 사진 중 제일 먼저 떠오르는 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족에게 받은 메시지 중 제일 따뜻했던 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "휴대폰 연락처에 가족들 이름을 뭐라고 저장하셨나요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "우리 집에 있는 가장 오래된 추억의 물건은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "본인의 어릴 적 귀요미 모습을 설명해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "부모님이 사준 물건 중 제일 아끼는 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "지금 보이는 우리 집 안 분위기는 한 단어로 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족 단톡방에 마지막으로 보낸 메시지는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "부모님(자식) 혈액형이랑 신발 사이즈를 맞히실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "우리 가족이 제일 자주 가는 단골 외식 메뉴는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족 중 제일 부지런한 사람과 게으른 사람은 누구인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "부모님이 제일 즐겨 듣는 노래 제목은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "제일 많이 듣는 대표 잔소리는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "우리 집에서 제일 귀여운 존재 1위는 누구인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "우리 가족만의 구호를 하나 새로 만들어볼까요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "평소 민망해서 못 했던 사랑 고백을 해볼까요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족들 때문에 속상했던 기억이 있으신가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "'우리 가족이라 참 다행이다'라고 느낀 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "남들에게 자랑하고 싶은 우리 가족의 장점은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "힘든 일 있을 때 가족이 해준 제일 큰 위로는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "받은 선물 중 평생 못 잊을 소중한 선물은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족 다 같이 가보고 싶은 여행지는 어디인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족을 단 하나의 단어로 표현한다면 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "가족과 함께 보낸 시간 중 제일 행복했던 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "엄마 아빠가 해준 요리 중 제일 맛있는 최애 음식은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.FAMILY, "올해가 지나기 전에 다 같이 이루고 싶은 소원은 무엇인가요?"),

            // 랜덤
            new QuestionSeed(GroupThemeCode.RANDOM, "밸런스 게임: 사랑 vs 우정, 3초 만에 고르고 이유를 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.RANDOM, "세상에서 제일 억울해서 미치기 직전인 표정을 지어볼까요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "주변 물건 하나를 집어서 킹받는 ASMR 소리를 내볼까요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "오직 표정만으로 '나 지금 배고파'를 표현해 보세요!"),
            new QuestionSeed(GroupThemeCode.RANDOM, "오늘 드신 음식 중에서 제일 맛있었던 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "10초 안에 '바'로 시작하는 단어 7개를 말씀해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "10초 동안 자기소개를 해본다면?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "지금 눈앞에 보이는 풍경을 말로 설명해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "오늘 입은 착장 스타일이 어떤 느낌인지 말씀해 주세요!"),
            new QuestionSeed(GroupThemeCode.RANDOM, "최근 쇼핑 장바구니에 담아둔 제일 사고 싶은 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "요새 빠져있는 노래 하나만 추천해주실 수 있나요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "인생에서 제일 좋아하는 최애 노래 3곡은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "본인의 이름 세 글자로 삼행시를 완성해 주시겠어요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "내가 제일 좋아하는 사람이 누구인지 말하고 이유도 말해주세요!"),
            new QuestionSeed(GroupThemeCode.RANDOM, "세상에서 제일 좋아하는 단어 한 개는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "오늘 하루 중 기분을 제일 좋게 만든 순간은 언제인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "요즘 본인을 가장 설레게 만들거나 재밌게 하는 건 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "들었던 칭찬 중 가장 기억에 남고 기분 좋은 말은 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "오늘의 TMI를 10초 동안 표현한다면?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "남들은 잘 모르는 본인만의 특이한 취향이나 습관이 있으신가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "내일 당장 24시간의 자유시간이 주어지면 무엇을 하고 싶으신가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "본인이 생각하는 스스로의 가장 큰 매력 포인트는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "돈이 생기면 제일 먼저 사고 싶은 위시리스트 1위는 무엇인가요?"),
            new QuestionSeed(GroupThemeCode.RANDOM, "올해가 지나기 전에 이루고 싶은 목표는 무엇인가요?")
    );

    private final QuestionSeedWriter seedWriter;

    @Override
    public void run(ApplicationArguments args) {
        int seeded = 0;
        for (QuestionSeed seed : SEEDS) {
            try {
                seedWriter.seed(seed.content(), seed.theme().name(), ANSWER_TIME_LIMIT_MS);
                seeded++;
            } catch (DataIntegrityViolationException e) {
                // 다른 인스턴스가 먼저 넣은 경우. 해당 질문의 트랜잭션만 롤백되므로 나머지는 계속 진행한다.
                log.debug("질문 seed 중복 - 무시합니다: {}", seed.content());
            }
        }
        log.info("질문 seed 처리 완료: {}건 (기존 데이터는 건너뜀)", seeded);
    }
}
