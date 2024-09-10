const urlParams = new URLSearchParams(window.location.search);
const id = urlParams.get('studyId');
$(document).ready(function (){
    $.ajax({
        type: "GET",
        url: "/algoy/study/get/" + id,
        success: function (data) {
            $('#title').val(data.title);

            let restoredText = data.content
                .replace(/<br>/g, '\n')
                .replace(/&nbsp;/g, ' ')
                .replace(/<\/?p>/g, '')
                .replace(/<\/?pre>/g, '');
            $('#content').val(restoredText);
            $('#max-participant').val(data.maxParticipant);
            $(`input:radio[name="status"][value="${data.status}"]`).prop('checked', true);
            $('#language').val(data.language);
        },
        error: function (error) {
            alert('조회에 실패했습니다.');
        }
    });
})

$('.btn-update').on('click', function(event) {
    event.preventDefault();
    // 기본 값이 존재하는 값 제외하고 입력 받은 값을 String 화하여 빈 값이면 입력 받음
    const title = $('#title').val().trim();
    const content = $('#content').val().trim();
    const maxParticipant = $('#max-participant').val().trim();
    const language = $('#language').val().trim();

    if (title === "") {
        alert('제목을 입력해주세요.');
        return;  // 제목이 비어있으면 요청 중단
    }
    if (content === "") {
        alert('본문을 입력해주세요.');
        return;  // 본문이 비어있으면 요청 중단
    }
    if (maxParticipant === "") {
        alert('링크를 입력해주세요');
        return;
    }
    if (language === "") {
        alert('시작 날짜를 입력해주세요');
        return;
    }

    if(title.length > 20) {
        alert('플랜 제목이 너무 깁니다.');
        $('#etc-input').val('');
        return;
    }


    let text = $('#content').val();

    var html = text
        .trim()
        .split('\n\n') // Separate paragraphs
        .map(function(paragraph) {
            return '<p>' + paragraph
                .replace(/\n/g, '<br>')
                .replace(/ /g, '&nbsp;') + '</p>';
        })
        .join('');

    // 이미지 html 태그 변환
    html = html.replace(/!\[(.*?)\]\((.*?)\)/g, '<img src="$2" alt="$1">');

    // 링크 html 태그 변환
    html = html.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2">$1</a>');

    // json 통신할 객체 생성
    let studyDto = {
        title: $('#title').val(),
        content: html,
        language: $('#language').val(),
        maxParticipant: $('#max-participant').val(),
        status: $(':radio[name="status"]:checked').val()
    };

    $.ajax({
        type: "POST",
        url: "/algoy/study/update/" + id,
        contentType: 'application/json',
        dataType: 'json',
        data: JSON.stringify(studyDto),
        success: function () {
            alert('수정에 성공했습니다.')
            location.href = "/algoy/study/detail?studyId=" + id;
        },
        error: function (error) {
            alert('수정에 실패했습니다.');
            // 실패 시 의도한 URL로 이동
            location.href = '/algoy/study/edit-form?studyId=' + id;
        }
    })
})

$('.search-button').click(function() {
    let keyword = $('.search-input').val(); // 검색어 입력받기
    if (keyword) {
        localStorage.setItem('searchKeyword', keyword); // 검색어를 localStorage에 저장
        window.location.href = '/algoy/study/main'; // 페이지 이동
    }
});