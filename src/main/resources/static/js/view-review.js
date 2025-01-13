const reviewId = Number(window.location.pathname.split('/').pop());
const reviewStatusKey = `review_${reviewId}`;
let isAscending = false;
let cachedComments = [];
let likeButton;
let bookmarkButton;
document.addEventListener('utilsInitiated', () => {
  fillCategoryPath(parseInt(document.querySelector('meta[name="_category_id"]').getAttribute("value")), 10);
  const reviewerAvatar = document.getElementById('reviewerAvatar');
  const currentURL = window.location.href;
  const commentInput = document.getElementById('commentInput');
  const commentButtons = document.getElementById('commentButtons');
  const cancelButton = document.getElementById('cancelComment');
  const sendButton = document.getElementById('sendComment');
  const currentUserAvatar = document.getElementById('currentUserAvatar');
  const anonymousCheck = document.getElementById('anonymousCheck');
  likeButton = document.getElementById('likeReviewBtn');
  bookmarkButton = document.getElementById('bookmarkReviewBtn');
  console.log(currentURL);
  // Обработка аватара
  if (reviewerAvatar) {
    if (reviewerAvatar.src === currentURL) {
      console.log('Avatar image is null');
      reviewerAvatar.replaceWith(generateAvatar(document.getElementById('reviewerName').textContent, 'avatar-md'));
    }
  }
  {
    let avatar;
    const userData = JSON.parse(sessionStorage.getItem('currentUser'));
    if (userData) {
      if (userData.avatarPath) {
        avatar = document.createElement('img');
        avatar.src = '/images/' + userData.avatarPath;
        avatar.className = 'avatar-sm rounded-circle';
      } else {
        avatar = generateAvatar(
          visibleName ?? userData.visibleName ?? userData.username,
          'avatar-sm');
      }
      avatar.id = 'currentUserAvatar';
      currentUserAvatar.replaceWith(avatar);
    }
  }


  const likeCountElement = document.getElementById('likeReviewCount');

  likeButton?.addEventListener('click', async () => {
    try {
      const response = await sendFetch(null, `/api/toggle-like/${reviewId}`, 'POST');
      if (response.ok) {
        let reviewStatus = sessionStorage.getItem(reviewStatusKey);
        if (!reviewStatus) await updateReviewStatus(reviewId);
        else {
          reviewStatus = JSON.parse(reviewStatus)
          reviewStatus.isLiked = response.isLiked;
          sessionStorage.setItem(reviewStatusKey, JSON.stringify(reviewStatus));
        }
        likeButton.classList.toggle('text-danger', response.isLiked);
        likeCountElement.textContent = formatLikes(response.likeCount);
      }
    } catch (error) {
      createPopup('Error toggling like: ' + error, messageLevel.ERROR);
    }
  });

  bookmarkButton?.addEventListener('click', () => toggleBookmark(bookmarkButton, reviewId));

  // Обработка поля комментария
  commentInput?.addEventListener('focus', () => {
    commentButtons.classList.remove('d-none');
    anonymousCheck.classList.remove('d-none');
  });

  cancelButton?.addEventListener('click', () => {
    commentInput.value = '';
    commentInput.blur();
    commentButtons.classList.add('d-none');
    anonymousCheck.classList.add('d-none');
    document.getElementById('makeAnonymous').checked = false;
  });

  commentInput?.addEventListener('input', () => {
    commentInput.style.height = 'auto'; // Сбрасываем высоту
    commentInput.style.height = commentInput.scrollHeight + 'px'; // Устанавливаем высоту в зависимости от содержимого
  });

  sendButton?.addEventListener('click', async () => {
    const commentText = commentInput.value.trim();
    if (!commentText) return;

    const isAnonymous = document.getElementById('makeAnonymous').checked;

    try {
      const response = await sendFetch({
        reviewId: reviewId,
        commentText: commentText,
        isAnonymous: isAnonymous
      }, '/api/save-comment', 'POST');

      if (response.ok) {
        const commentsList = document.querySelector('#commentsSection ul');
        const emptyMessage = commentsList.querySelector('.alert');
        if (emptyMessage) {
          commentsList.innerHTML = '';
        }
        const comment = response;
        // Добавляем новый комментарий в начало списка
        const newCommentHtml = makeComment(
          comment.user.username,
          comment.commentText,
          comment.user.avatarPath,
          comment.createdAt,
          comment.isAnonymous,
          comment.user.id
        );
        cachedComments.push(comment);
        commentsList.insertAdjacentHTML('afterbegin', newCommentHtml);

        commentInput.value = '';
        commentInput.dispatchEvent(new Event('input'));
        commentButtons.classList.add('d-none');
        anonymousCheck.classList.add('d-none');
      }
    } catch (error) {
      createPopup('Error saving comment: ' + error, messageLevel.ERROR);
    }
  });

  document.getElementById('refreshComments')?.addEventListener('click',
    () => loadComments(reviewId));

  const submitReportButton = document.getElementById('submitReportBtn');
  const reportDescription = document.getElementById('reportDescription');
  submitReportButton?.addEventListener('click', async () => {
    const description = reportDescription.value.trim();
    if (!description) {
      createPopup('Please provide a description for the report.', messageLevel.WARNING);
      return;
    }

    try {
      const response = await sendFetch({reviewId: reviewId, description: description}, '/api/report-review', 'POST');
      if (response.ok) {
        createPopup('Report submitted successfully!', messageLevel.SUCCESS);
        // Закрыть модальное окно
        const modal = bootstrap.Modal.getInstance(document.getElementById('reportModal'));
        modal.hide();
      } else {
        createPopup('Failed to submit report: ' + response.response, messageLevel.ERROR);
      }
    } catch (error) {
      createPopup('Error submitting report: ' + error, messageLevel.ERROR);
    }
  });

  document.getElementById('sortComments')?.addEventListener('click', (event) => {
    isAscending = event.target.checked;
    sortComments();

    updateCommentsSection(cachedComments);
  });

  updateReviewStatus(reviewId).then(_ => {
  });
  // Существующий код загрузки комментариев
  loadComments(reviewId).then(_ => {
  });

}, {once: true});


function sortComments() {
  cachedComments.sort((a, b) => {
    const dateA = new Date(a.createdAt);
    const dateB = new Date(b.createdAt);
    return isAscending ? dateA - dateB : dateB - dateA;
  });
}

async function loadComments(reviewId) {
  try {
    const response = await fetch(`/api/public/get-comments/${reviewId}`);
    if (response.ok) {
      cachedComments = await response.json();
      updateCommentsSection(cachedComments);
    }
  } catch (error) {
    createPopup('Error loading comments: ' + error, messageLevel.WARNING);
  }
}

function makeComment(username, text, avatarPath, date, isAnonymous, userId) {
  let userHref = `href="/user/${userId}"`;
  let avatar = avatarPath ?
    `<img class="avatar-sm rounded-circle me-1" src="/images/${avatarPath}" alt="Avatar for ${username}">` :
    generateAvatar(username, 'avatar-sm me-1').outerHTML;
  if (userId === -1) {
    username = 'Deleted User';
    userHref = '';
    avatar = `<div class="avatar-sm rounded-circle me-1 bg-dark-subtle flex-center text-white fw-bolder">D</div>`;
  } else if (isAnonymous) {
    username = 'Anonymous User';
    userHref = '';
    avatar = `<div class="avatar-sm rounded-circle me-1 bg-dark flex-center text-white fw-bolder">A</div>`;
  }
  return `<li class="list-group-item d-flex gap-1">
            <a class="text-decoration-none text-black" ${userHref}>${avatar}</a>
            <div>
              <div class="lh-1 align-top">
                <a class="text-decoration-none text-black me-1 fw-semibold" ${userHref}>${username}</a>
                <span class="date-md">${getRelativeTime(date)}</span>
              </div>
              <div class="pre-wrap">${text}</div>
            </div>
          </li>`;
}

function updateCommentsSection(comments) {
  const commentsList = document.querySelector('#commentsSection ul');
  if (commentsList) {
    sortComments();
    if (comments.length > 0)
      commentsList.innerHTML = comments.map(comment => makeComment(
        comment.user.username,
        comment.commentText,
        comment.user.avatarPath,
        comment.createdAt,
        comment.isAnonymous,
        comment.user.id
      )).join('');
    else {
      commentsList.innerHTML = `
              <li class="list-group-item">
                  <p class="m-0 text-center">No comments yet. Be first!</p>
              </li>`
    }
  }

}