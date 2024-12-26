// manager.js
let page = 0;
const size = 10;
let loadMoreReviewsBtn;
let pages = {};
let reportsSection;
let analysesSection;
document.addEventListener('DOMContentLoaded', () => {
  let loadMoreReviewsBtn = document.getElementById('refreshReports');
  reportsSection = document.getElementById('reportsSection');
  analysesSection = document.getElementById('analysesSection');
  loadMoreReviewsBtn?.addEventListener('click', loadMoreReviews);
  loadMoreReviews().then(_ => {
  });
});

async function loadMoreReviews() {
  try {
    const response = await sendFetch(null, `/api/get-reports?page=${page}&size=${size}`, 'GET');
    if (response.ok) {
      //result:{reviews:[{review:ReviewDTO, reports:[], page:0, totalPages:N}...],hasNext:bool,totalPages:N,page:M}
      response.result.reviews.forEach(el => {
        console.log(el.reports);
        if (reportsSection.querySelector(`#reports-${el.review.id}`)) return;
        const reviewElement = createReviewElement(el.review, el.reports);
        reportsSection.insertBefore(reviewElement, loadMoreReviewsBtn);
      });
      if (!response.result.hasNext) {
        createPopup('Nothing more to load :(', messageLevel.INFO);
      }
      page = Math.min(response.result.totalPages, (response.result.totalItems > size * (1 + page) ? 1 : 0) + page);
    } else {
      createPopup(response.response, messageLevel.ERROR);
    }
  } catch (error) {
    createPopup('Error loading reviews: ' + error, messageLevel.ERROR);
  }
}

async function loadMoreReports(reviewId, button) {
  try {
    if (button.classList.contains('collapsed')) return;
    let ul = reportsSection.querySelector(`#reports-${reviewId} ul`);
    if (!ul || ul.childElementCount > 5) return;
    let page = pages[`review_${reviewId}`] || 0;
    const response = await sendFetch(null, `/api/get-reports/${reviewId}?page=${page}&size=${size}`, 'GET');
    if (response.ok) {
      //result:{reviews:[{review:ReviewDTO, reports:[], page:0, totalPages:N}...],hasNext:bool,totalPages:N,page:M}
      console.log(response.reports);
      const badge = button.querySelector('.badge');
      badge.textContent = response.totalItems;
      ul.innerHTML = createReportsLis(response.reports);

      if (!response.hasNext) {
        createPopup('Nothing more to load :(', messageLevel.INFO);
      }
      pages[`review_${reviewId}`] = Math.min(response.totalPages, (response.totalItems > size * (1 + page) ? 1 : 0) + page);

    } else {
      createPopup(response.response, messageLevel.ERROR);
    }
  } catch (error) {
    createPopup('Error loading reviews: ' + error, messageLevel.ERROR);
  }
}

function createReportsLis(reports) {
  return reports.map(report => `
          <li class="list-group-item d-flex justify-content-between align-items-center">
              <div>
                  <p class="mb-1">${report.description}</p>
                  <small class="text-muted">${getRelativeTime(report.createdAt)}</small>
              </div>
              <button class="btn btn-sm btn-success" onclick="queueReportResolve(${report.id},this)">
                  Resolve
              </button>
          </li>
        `).join('');
}

async function analyseWithAI(reviewId, btn, regenerate = false) {
  let prevAnalysis = analysesSection.querySelector(`#analysis_${reviewId}`);
  if (!regenerate && prevAnalysis) {
    const modal = new bootstrap.Modal(prevAnalysis);
    modal.show();
  } else {

    createPopup("Sent for analysis", messageLevel.INFO);
    try {
      const response = await sendFetch({reviewId: reviewId}, "/api/analyse-review", 'post');
      if (response.ok && !response.redirected) {
        console.log(response.response);
        createModalWithResponse(response.response, reviewId, btn);
        if (regenerate && prevAnalysis) prevAnalysis.remove();
        btn?.classList.remove('btn-outline-primary');
        btn?.classList.add('btn-primary');
        createPopup(`Analysed review ${reviewId} <a href="#" data-bs-toggle="modal" data-bs-target="#analysis_${reviewId}">here!</a>`, messageLevel.SUCCESS);
      } else {
        createPopup("Failed to analyse", messageLevel.WARNING);
      }
    } catch (e) {
      createPopup("Error while analysing: " + e, messageLevel.ERROR);
    }
  }

}

function createModalWithResponse(text, reviewId, btn) {
  const modalHtml = `
    <div class="modal fade" id="analysis_${reviewId}" tabindex="-1" aria-labelledby="analysis_${reviewId}Label" aria-hidden="true">
      <div class="modal-dialog modal-dialog-scrollable">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="analysis_${reviewId}Label">Analysis for review ${reviewId}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body">
            ${text}
          </div>
          <div class="modal-footer flex-vcenter justify-content-between">
            <button type="button" class="btn btn-outline-danger">Delete Analysis</button>
            <button type="button" class="btn btn-outline-secondary" onclick="analyseWithAI(${reviewId},this,true)">Regenerate</button>
          </div>
        </div>
      </div>
    </div>
  `;

  // Добавляем модальное окно в DOM
  analysesSection.insertAdjacentHTML('beforeend', modalHtml);

  // Инициализация и отображение модального окна
  const modalElement = analysesSection.querySelector(`#analysis_${reviewId}`);

  // Удаляем модальное окно из DOM при закрытии
  modalElement.querySelector('.modal-footer button').addEventListener('click', () => {
    modalElement.remove();
    btn.classList.add('btn-outline-primary');
    btn.classList.remove('btn-primary');
  });
}


function createReviewElement(review, reports) {
  const reviewDiv = document.createElement('div');
  const reportsId = `reports-${review.id}`;
  let rating = Number(review.rating);
  let imagesLeft = Number(review.totalImages) - 3;
  let imagesLeftCloud = imagesLeft > 0 ?
    `<p class="alert bg-white end-0 m-0 p-2 position-absolute rounded-5
text-center text-dark top-50 translate-middle">+${formatLikes(imagesLeft)} Photo${imagesLeft > 1 ? 's' : ''}</p>` : '';
  let imagePaths = review.imagePaths;
  let avatar = review.authorAvatarPath ?
    `<img src="/images/${review.authorAvatarPath}" class="avatar-xs me-1 rounded-circle">` :
    generateAvatar(review.authorName, 'avatar-xs me-1').outerHTML;
  let userHref = review.authorId !== -1 ? `href="/user/${review.authorId}"` : '';
  let images = '';
  for (let i = imagePaths.length; i < 3; ++i) {
    images += `<div class="border-1 rounded-1 p-0 overflow-hidden ms-1 gallery-item"></div>`
  }
  imagePaths.forEach(src =>
    images += `<img class="border-1 rounded-1 p-0 overflow-hidden ms-1 gallery-item" src="images\\${src}">`);
  reviewDiv.innerHTML = `
<div class="card mb-4">
<div class="card-header flex-vcenter justify-content-between">
  <h4 class="m-0"><a href="/review/${review.id}" class="text-decoration-none text-black">${review.title}</a></h4>
  <span class="fw-bolder m-0"><span class="align-text-top h5 lh-1 mb-0 me-1">${review.likes}</span><i class="bi bi-heart-fill align-baseline text-danger"></i></span>
</div>
<div class="card-body row border-bottom">
<div class="col-12 col-md-6 float-start"><h5>${review.shortReview}</h5><p>${review.reviewTeaser}</p></div>
<div class="col-12 col-md-6 d-flex d-inline-flex justify-content-end position-relative">${imagesLeftCloud}${images}</div>
</div>
<div class="card-body pt-2 pb-2 d-flex justify-content-between align-items-center">
  <span class="star-rating">${'<i class="bi bi-star-fill star-off"></i>'.repeat(5 - rating)}${'<i class="bi bi-star-fill star-on"></i>'.repeat(rating)}
</span>
  <a ${userHref} class="d-flex text-decoration-none text-black">${avatar}${review.authorName}</a>
  <span class="date">${formatDateTime(review.createdAt)}</span>
</div>
<div class="card-footer d-flex justify-content-between align-items-center">
  <span>
    <button class="btn btn-outline-primary"
            type="button"
            data-bs-toggle="collapse"
            data-bs-target="#${reportsId}"
            aria-expanded="false"
            aria-controls="${reportsId}"
            onclick="loadMoreReports(${review.id}, this)">
      Show Reports <span class="badge bg-secondary">${reports.length}</span>
    </button>
    <button class="ms-1 btn btn-outline-primary btn-square" onclick="analyseWithAI(${review.id},this)"><i class="bi bi-stars"></i></button>
  </span>
  ${reports.length > 0 ?
    `<button class="btn btn-outline-success"
onclick="resolveAllReports(${review.id})">Resolve All Reports</button>` : ''}
  <div class="input-group" style="max-width: 400px;">
        <select class="form-select" id="status-${review.id}">
            <option value="PUBLISHED" ${review.status === 'PUBLISHED' ? 'selected' : ''}>Published</option>
            <option value="MODERATION" ${review.status === 'MODERATION' ? 'selected' : ''}>Moderation</option>
            <option value="HIDDEN" ${review.status === 'HIDDEN' ? 'selected' : ''}>Hidden</option>
        </select>
        <button class="btn btn-outline-secondary" onclick="updateStatus(${review.id})">
            Update Status
        </button>
    </div>
</div>
</div>
<div class="collapse mt-3 mb-5" id="${reportsId}">
    <div class="card card-body">
      <h6>Reports:</h6>
      <ul class="list-group">
      ${createReportsLis(reports)}
      </ul>
    </div>
  </div>
`;

  return reviewDiv;
}

let resolveTimer = null;

function startResolveTimer() {
  if (resolveTimer === null && reportsToResolve.size > 0) {
    resolveTimer = setInterval(sendResolveReports, 30000); // 60000 мс = 1 минута
  }
}

// В начало manager.js добавим:
let reportsToResolve = new Map(); // Меняем Set на Map для хранения пар reportId -> parentElement

async function sendResolveReports() {
  if (reportsToResolve.size === 0) {
    clearInterval(resolveTimer);
    resolveTimer = null;
    return;
  }

  try {
    const response = await sendFetch({
      reportIds: Array.from(reportsToResolve.keys())
    }, '/api/resolve-reports', 'POST');

    if (response.ok) {
      // Удаляем решенные репорты из DOM
      reportsToResolve.forEach((listItem, reportId) => {
        // Сначала получаем id обзора из родительского контейнера репортов
        const reportsCard = listItem.closest('.collapse');
        const reportsId = reportsCard.id;
        const reviewId = reportsId.replace('reports-', '');

        // Теперь можем найти кнопку показа репортов для этого обзора
        const showReportsBtn = document.querySelector(`button[data-bs-target="#${reportsId}"]`);
        const badge = showReportsBtn.querySelector('.badge');
        const currentCount = parseInt(badge.textContent) - 1;

        // Обновляем счетчик и удаляем элемент
        badge.textContent = currentCount;
        listItem.remove();

        // Если репортов не осталось, скрываем кнопку resolve all
        if (currentCount === 0) {
          document.querySelector(`[onclick="resolveAllReports(${reviewId})"]`)?.remove();
        }
      });

      createPopup('Reports resolved successfully', messageLevel.SUCCESS);
      reportsToResolve.clear();
    } else {
      createPopup(response.response, messageLevel.ERROR);
    }
  } catch (error) {
    createPopup('Error resolving reports: ' + error, messageLevel.ERROR);
  }
}

function queueReportResolve(reportId) {
  const button = event.target;
  const listItem = button.closest('.list-group-item');

  button.classList.remove('btn-success');
  button.classList.add('btn-outline-success');
  button.disabled = true;

  reportsToResolve.set(reportId, listItem);
  startResolveTimer();
  createPopup('Report queued for resolution', messageLevel.SUCCESS);
}

async function resolveAllReports(reviewId) {
  try {
    const button = event.target;
    button.classList.remove('btn-success');
    button.classList.add('btn-outline-success');
    button.disabled = true;

    const reports = document.querySelectorAll(`#reports-${reviewId} .list-group-item`);
    reports.forEach(report => {
      const reportBtn = report.querySelector('button');
      reportBtn.classList.remove('btn-success');
      reportBtn.classList.add('btn-outline-success');
      reportBtn.disabled = true;

      const reportId = parseInt(reportBtn.onclick.toString().match(/\d+/)[0]);
      reportsToResolve.set(reportId, report);
    });

    startResolveTimer();
    createPopup('All reports queued for resolution', messageLevel.SUCCESS);
  } catch (error) {
    createPopup('Error queueing reports: ' + error, messageLevel.ERROR);
  }
}


async function updateStatus(reviewId) {
  const status = document.getElementById(`status-${reviewId}`).value;
  try {
    const response = await sendFetch({
      reviewId: reviewId,
      status: status
    }, '/api/update-review-status', 'POST');

    if (response.ok) {
      createPopup('Status updated successfully', messageLevel.SUCCESS);
    } else {
      createPopup(response.response, messageLevel.ERROR);
    }
  } catch (error) {
    createPopup('Error updating status: ' + error, messageLevel.ERROR);
  }
}