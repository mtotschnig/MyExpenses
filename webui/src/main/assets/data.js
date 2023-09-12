const dateMode = {
  date: 1,
  dateTime: 2,
  bookingValue: 3
};

document.addEventListener('alpine:init', () => {
    document.title = messages.app_name + " " + messages.title_webui
    let date = new Date();
    let dateFormatted = formatDate(date);
    let categoryTreeDepth = ${category_tree_depth};
    Alpine.data('modelData', () => ({
        loading: false,
        id: 0,
        signum: false,
        amount: '',
        date: dateFormatted,
        valueDate: dateFormatted,
        time: formatTime(date),
        party: 0,
        account: 0,
        comment: '',
        categoryTreeDepth: categoryTreeDepth,
        categoryPath:  Array.from(Array(categoryTreeDepth), () => { return { id: 0 }; }),
        selectedTags: [],
        method: 0,
        number: '',
        resultText: '',
        resultCode: 0,
        data: ${data},
        transactions: [],
        activeTransaction :null,
        async loadTransaction(transaction) {
            this.signum = transaction.amount > 0 ? true : false;
            this.amount = Math.abs(transaction.amount);
            this.party = transaction.party;
            this.comment = transaction.comment;
            this.number = transaction.number;
            this.date = transaction.date;
            this.time = transaction.time;
            this.valueDate = transaction.valueDate;
            if (transaction.category > 0) {
                let newPath = this.pathFromTerminalNode(transaction.category)
                let lastIndex = newPath.length - 1
                for (let index = lastIndex; index >= 0; index--) {
                    this.categoryPath[lastIndex - index].id = newPath[index].id;
                    await this.$nextTick();
                }
            } else {
                this.resetCategory();
            }
            this.selectedTags = transaction.tags;
            this.method = transaction.method
        },
        submitForm() {
            let data = {
                account: this.account,
                amount: this.signum ? this.amount : -this.amount,
                date: this.date,
                time: this.currentDateMode == dateMode.dateTime ? this.time : null,
                valueDate: this.valueDate,
                party: this.party,
                category: this.terminalNodeFromPath(this.categoryPath),
                tags: this.selectedTags,
                comment: this.comment,
                method: this.method,
                number: this.number
            }
            let invalidFields = [];
            if (data.date == '') {
                invalidFields.push(this.currentDateMode == dateMode.bookingValue ? messages.booking_date : messages.date)
            }
            if (this.currentDateMode == dateMode.dateTime && data.time == '') {
                invalidFields.push(messages.time)
            }
            if (this.currentDateMode == dateMode.bookingValue && data.valueDate == '') {
                invalidFields.push(messages.value_date)
            }
            if (invalidFields.length > 0) {
                this.errorHandler(Error(messages.validate_error_not_empty + ": " + invalidFields.join(', ')));
                return;
            }

            let uri = "/transactions" + (this.id == 0 ? "" : ("/" + this.id))
            let method = this.id == 0 ? "POST" : "PUT"
            fetch(uri, {
                headers: {
                    'Content-Type': 'application/json'
                },
                method: method,
                body: JSON.stringify(data)
            }).then(response => {
                this.resultCode = response.status
                if (!response.ok) {
                    throw Error(response.statusText);
                }
                response.text().then(text => this.resultText = text)
                this.loadTransactions();
            }).catch((error) => { this.errorHandler(error); });
        },
        reset() {
            if (confirm(messages.dialog_confirm_discard_changes)) {
                let now = new Date();
                let dateFormatted = formatDate(now);
                this.id = 0;
                this.amount = '';
                this.date = dateFormatted;
                this.valueDate = dateFormatted;
                this.time = formatTime(now);
                this.party = 0;
                this.comment = '';
                this.resetCategory();
                this.selectedTags = [];
                this.method = 0;
                this.number = '';
                this.resultText = '';
                this.resultCode = 0;
            }
        },
        get accountType() {
            return this.data.accounts.find(item => item.id == this.account).type
        },
        get currentDateMode() {
            if (!(this.accountType == "CASH")) {
              if (${withValueDate}) {
                return dateMode.bookingValue;
              }
            }
            return ${withTime} ? dateMode.dateTime : dateMode.date;
        },
        get dateRowLabel() {
            switch(this.currentDateMode) {
              case dateMode.date: return messages.date;
              case dateMode.dateTime: return messages.date + " / " + messages.time;
              case dateMode.bookingValue: return messages.booking_date + " / " + messages.value_date;
            }
        },
        get accountCurrency() {
            return this.data.accounts.find(item => item.id == this.account).currency;
        },
        get accountLabel() {
            return this.data.accounts.find(item => item.id == this.account).label;
        },
        get methodsForTypes() {
            return this.data.methods.filter(item => item.accountTypes.includes(this.accountType) && (item.type == 0 || item.type == (this.signum ? 1 : -1)));
        },
        categoriesByParent(level) {
            let parent = level == 0 ? null : this.categoryPath[level-1].id
            return this.data.categories.filter(item => item.parent == parent)
        },
        terminalNodeFromPath(path) {
          return path.reduce((acc, curr) => curr.id == 0 ? acc : curr.id, null);
        },
        pathFromTerminalNode(category) {
            let result = [];
            let node = category;
            while(true) {
                result.push( { id: node } );
                node = this.data.categories.find(item => item.id == node).parent;
                if (node == undefined) break;
            }
            return result
        },
        resetCategory() {
            if (this.categoryPath.length > 0) { this.categoryPath[0].id = 0; };
        },
        loadTransactions() {
            this.loading = true
            this.transactions = []
            fetch("/transactions?account_id=" + this.account, {
                headers: {
                    'Content-Type': 'application/json'
                },
                method: 'GET'
            }).then(response => {
                this.loading = false;
                this.resultCode = response.status;
                if (!response.ok) {
                    throw Error(response.statusText);
                }
                response.json().then(data => { this.transactions = data } );
            }).catch((error) => { this.errorHandler(error); });
        },
        menu : [
            {
                id: 'edit',
                icon: 'edit',
                label: 'menu_edit'
            },
            {
                id: 'clone',
                icon: 'content_copy',
                label: 'menu_clone_transaction'
            },
            {
                id: 'delete',
                icon: 'delete',
                label: 'menu_delete'
            }
        ],
        contextAction(menuId) {
            switch(menuId) {
                case "edit": {
                    this.loadTransaction(this.activeTransaction);
                    this.id = this.activeTransaction.id;
                    break;
                }
                case "clone": {
                    this.loadTransaction(this.activeTransaction);
                    this.id = 0;
                    break;
                }
                case "delete": {
                    if (confirm(messages.warning_delete_transaction)) {
                        fetch("/transactions/" + this.activeTransaction.id, {
                            method: 'DELETE'
                        }).then(response => {
                            this.resultCode = response.status;
                            if (!response.ok) {
                                    throw Error(response.statusText);
                            }
                            response.text().then(text => {
                                this.resultText = text
                            });
                            this.loadTransactions();
                        }).catch((error) => { this.errorHandler(error); });
                    }
                    break;
                }
            }
            this.activeTransaction = null
        },
        errorHandler(error) {
             console.error(error);
             this.resultText = error.message
        },
        isEditable(transaction) {
            return transaction.category != 0 && transaction.transferPeer == undefined
        },
        init() {
            this.account = this.data.accounts[0].id;
            this.$watch('account', _ => {
                this.loadTransactions();
                if (this.id > 0) alert(message("webui_warning_move_transaction", {account: this.accountLabel}));
            });
            this.$watch('amount', value => { if (value < 0) amount = -value } );
            ${categoryWatchers}
            this.loadTransactions();
        }
    }))
})

function formatPart(number) {
    return number.toString().padStart(2, '0')
}

function formatDate(date) {
  let month = formatPart(date.getMonth() + 1);
  let day = formatPart(date.getDate());
  let year = date.getFullYear();
  return [year, month, day].join('-');
}

function formatTime(date) {
  let hours = formatPart(date.getHours());
  let minutes = formatPart(date.getMinutes());
  return [hours, minutes].join(':');
}

function positionPopup(event, menu) {
    menu.style.top = event.pageY + "px"
    menu.style.left = event.pageX + "px"
}

function message(message, data) {
  const pattern = /{\s*(\w+?)\s*}/g;
  return messages[message].replace(pattern, (_, token) => data[token] || '');
}
