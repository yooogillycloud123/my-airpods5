"""J Memo Personal — independent offline sticky-note application.

Memoit is NOT modified or loaded. This project stores data only in JMemoPersonal.
Requires only Python 3.11+ standard library while running from source.
"""
from __future__ import annotations

import json
import os
import shutil
import tempfile
import uuid
from datetime import datetime
from pathlib import Path
import tkinter as tk
from tkinter import messagebox, simpledialog, filedialog

COLORS = ("#FFF5B8", "#DFF1FC", "#E8F4D8", "#FBE3EB", "#E9E1FA", "#FFFFFF")
FONT = "맑은 고딕"


def data_folder() -> Path:
    if os.environ.get("JMEMO_HOME"):
        return Path(os.environ["JMEMO_HOME"]).expanduser().resolve()
    root = os.environ.get("APPDATA") if os.name == "nt" else None
    return Path(root) / "JMemoPersonal" if root else Path.home() / ".jmemo_personal"


def new_note(x=140, y=140):
    timestamp = datetime.now().isoformat(timespec="seconds")
    return dict(id=uuid.uuid4().hex, text="", group="기본", color=COLORS[0],
                x=x, y=y, width=300, height=260, visible=True, pinned=True,
                collapsed=False, favorite=False, deleted=False, opacity=0.97,
                due="", notified=False, created_at=timestamp, updated_at=timestamp)


def validate(data):
    if not isinstance(data, dict) or data.get("schema") != 1 or not isinstance(data.get("notes"), list):
        raise ValueError("지원하지 않거나 손상된 J Memo 저장 파일")
    ids = set()
    for note in data["notes"]:
        if not isinstance(note, dict) or not isinstance(note.get("id"), str) or not isinstance(note.get("text"), str):
            raise ValueError("메모 구조가 올바르지 않습니다")
        if not note["id"] or note["id"] in ids:
            raise ValueError("중복/누락된 메모 ID")
        ids.add(note["id"])


class Store:
    def __init__(self, folder=None):
        self.folder = Path(folder) if folder else data_folder()
        self.folder.mkdir(parents=True, exist_ok=True)
        self.path = self.folder / "notes.json"
        self.backups = self.folder / "backups"
        self.backups.mkdir(exist_ok=True)
        self.data = dict(schema=1, notes=[], prefs={})
        if self.path.exists():
            try:
                candidate = json.loads(self.path.read_text(encoding="utf-8"))
                validate(candidate)
            except (ValueError, OSError, UnicodeError) as exc:
                raise RuntimeError("기존 메모 데이터 읽기 실패. 덮어쓰기를 차단합니다: " + str(exc)) from exc
            self.data = candidate
            self.backup("startup")

    @property
    def notes(self):
        return self.data["notes"]

    def save(self):
        validate(self.data)
        output = json.dumps(self.data, ensure_ascii=False, indent=2).encode("utf-8")
        fd, temp_path = tempfile.mkstemp(dir=self.folder, prefix=".jmemo_", suffix=".tmp")
        try:
            with os.fdopen(fd, "wb") as writer:
                writer.write(output)
                writer.flush()
                os.fsync(writer.fileno())
            os.replace(temp_path, self.path)
        finally:
            if os.path.exists(temp_path):
                os.unlink(temp_path)

    def backup(self, label="manual"):
        if not self.path.exists():
            return None
        path = self.backups / ("jmemo_" + datetime.now().strftime("%Y%m%d_%H%M%S_%f") + "_" + label + ".json")
        shutil.copy2(self.path, path)
        return path

    def create(self, **kwargs):
        note = new_note(**kwargs)
        self.notes.append(note)
        self.save()
        return note

    def find(self, identifier):
        return next((n for n in self.notes if n["id"] == identifier), None)


class Sticky:
    def __init__(self, app, note):
        self.app, self.note = app, note
        self.window = tk.Toplevel(app.root)
        self.window.overrideredirect(True)
        self.window.configure(bg=note.get("color", COLORS[0]), highlightbackground="#9FA6AE", highlightthickness=1)
        self.window.wm_attributes("-topmost", note.get("pinned", True))
        try:
            self.window.wm_attributes("-alpha", float(note.get("opacity", 0.97)))
        except tk.TclError:
            pass
        self.drag_origin = None
        self.head = tk.Frame(self.window, height=32, bg=note.get("color", COLORS[0]), cursor="fleur")
        self.head.pack(fill="x")
        self.head.pack_propagate(False)
        self.title = tk.Label(self.head, text=self.caption(), font=(FONT, 10, "bold"), anchor="w",
                              bg=note.get("color", COLORS[0]), fg="#273342", padx=9)
        self.title.pack(side="left", fill="x", expand=True)
        for label, action in (("×", self.hide), ("▴", self.fold), ("●", self.choose_color), ("★", self.favorite), ("⋯", self.menu)):
            b = tk.Button(self.head, text=label, command=action, relief="flat", bd=0,
                          bg=note.get("color", COLORS[0]), font=(FONT, 10))
            b.pack(side="right")
        self.body = tk.Frame(self.window, bg=note.get("color", COLORS[0]))
        self.text = tk.Text(self.body, wrap="word", undo=True, borderwidth=0, padx=9, pady=7,
                            font=(FONT, 11), bg=note.get("color", COLORS[0]))
        self.text.insert("1.0", note.get("text", ""))
        self.text.pack(fill="both", expand=True)
        self.footer = tk.Frame(self.body, height=17, bg=note.get("color", COLORS[0]))
        self.footer.pack(fill="x")
        self.grip = tk.Label(self.footer, text="◢", bg=note.get("color", COLORS[0]), cursor="sizing")
        self.grip.pack(side="right")
        self.grip.bind("<ButtonPress-1>", self.resize_begin)
        self.grip.bind("<B1-Motion>", self.resize_move)
        for item in (self.head, self.title):
            item.bind("<ButtonPress-1>", self.move_begin)
            item.bind("<B1-Motion>", self.move_do)
            item.bind("<ButtonRelease-1>", self.move_end)
            item.bind("<Double-Button-1>", lambda event: self.fold())
        self.text.bind("<<Modified>>", self.changed)
        self.window.protocol("WM_DELETE_WINDOW", self.hide)
        self.refresh_geometry()
        self.body_visibility()
        self.popup = tk.Menu(self.window, tearoff=False)
        self.popup.add_command(label="메모 제목/본문 복사", command=self.copy_text)
        self.popup.add_command(label="그룹 지정", command=self.group)
        self.popup.add_command(label="항상 위 고정 전환", command=self.pin)
        self.popup.add_command(label="메모 복제", command=lambda: app.duplicate(note))
        self.popup.add_command(label="휴지통으로 이동", command=self.trash)
        self.text.bind("<Button-3>", self.menu)

    def caption(self):
        return next((line[:35] for line in self.note.get("text", "").splitlines() if line.strip()), "새 메모")

    def refresh_geometry(self):
        n = self.note
        self.window.geometry(f'{max(210,int(n.get("width",300)))}x{34 if n.get("collapsed") else max(120,int(n.get("height",260)))}+{int(n.get("x",140))}+{int(n.get("y",140))}')

    def body_visibility(self):
        if self.note.get("collapsed"):
            self.body.pack_forget()
        else:
            self.body.pack(fill="both", expand=True)

    def changed(self, event=None):
        if not self.text.edit_modified():
            return
        self.text.edit_modified(False)
        self.app.mark_dirty(self.note["id"])

    def flush(self):
        self.note["text"] = self.text.get("1.0", "end-1c")
        self.note["updated_at"] = datetime.now().isoformat(timespec="seconds")
        self.title.configure(text=self.caption())

    def move_begin(self, event):
        self.drag_origin = (event.x_root, event.y_root, self.window.winfo_x(), self.window.winfo_y())

    def move_do(self, event):
        if not self.drag_origin:
            return
        x, y, old_x, old_y = self.drag_origin
        self.window.geometry(f'+{old_x + event.x_root-x}+{old_y + event.y_root-y}')

    def move_end(self, event=None):
        self.note["x"], self.note["y"] = self.window.winfo_x(), self.window.winfo_y()
        self.drag_origin = None
        self.app.store.save()

    def resize_begin(self, event):
        self.resize_origin = (event.x_root, event.y_root, self.window.winfo_width(), self.window.winfo_height())

    def resize_move(self, event):
        x, y, w, h = self.resize_origin
        nw, nh = max(210, w + event.x_root-x), max(120, h + event.y_root-y)
        self.window.geometry(f"{nw}x{nh}")
        self.note["width"], self.note["height"] = nw, nh
        self.app.mark_dirty(self.note["id"])

    def hide(self):
        self.flush()
        self.note["visible"] = False
        self.window.withdraw()
        self.app.store.save()
        self.app.refresh()

    def fold(self):
        self.flush()
        if not self.note.get("collapsed"):
            self.note["height"] = max(120, self.window.winfo_height())
        self.note["collapsed"] = not self.note.get("collapsed", False)
        self.body_visibility()
        self.refresh_geometry()
        self.app.store.save()

    def choose_color(self):
        index = COLORS.index(self.note.get("color", COLORS[0])) if self.note.get("color") in COLORS else 0
        color = COLORS[(index+1) % len(COLORS)]
        self.note["color"] = color
        for widget in (self.window, self.head, self.title, self.body, self.footer, self.grip, self.text):
            widget.configure(bg=color)
        for child in self.head.winfo_children():
            child.configure(bg=color)
        self.app.store.save()

    def favorite(self):
        self.note["favorite"] = not self.note.get("favorite", False)
        self.app.store.save()
        self.app.refresh()

    def group(self):
        name = simpledialog.askstring("그룹 지정", "그룹 이름:", initialvalue=self.note.get("group","기본"), parent=self.window)
        if name is not None and name.strip():
            self.note["group"] = name.strip()
            self.app.store.save()
            self.app.refresh()

    def pin(self):
        self.note["pinned"] = not self.note.get("pinned", True)
        self.window.wm_attributes("-topmost", self.note["pinned"])
        self.app.store.save()

    def trash(self):
        if messagebox.askyesno("메모 휴지통", "휴지통으로 이동할까요? 복원할 수 있습니다.", parent=self.window):
            self.flush()
            self.note["deleted"] = True
            self.window.destroy()
            self.app.windows.pop(self.note["id"], None)
            self.app.store.save()
            self.app.refresh()

    def copy_text(self):
        self.window.clipboard_clear()
        self.window.clipboard_append(self.text.get("1.0", "end-1c"))

    def menu(self, event=None):
        self.popup.tk_popup(event.x_root if event else self.window.winfo_rootx()+100,
                            event.y_root if event else self.window.winfo_rooty()+35)


class App:
    def __init__(self, root, store):
        self.root, self.store = root, store
        self.windows = {}
        self.pending = set()
        self.timer = None
        root.title("J Memo Personal | 메모 관리자")
        root.geometry("575x545")
        root.configure(bg="#F5F6FA")
        toolbar = tk.Frame(root, bg="#F5F6FA")
        toolbar.pack(fill="x", padx=12, pady=12)
        for text, func in (("+ 새 메모", self.add), ("모두 표시", self.show_all), ("모두 숨김", self.hide_all),
                           ("백업", self.backup), ("휴지통", self.trash_view)):
            tk.Button(toolbar, text=text, command=func, padx=7).pack(side="left", padx=3)
        self.query = tk.StringVar()
        tk.Entry(root, textvariable=self.query, font=(FONT, 11)).pack(fill="x", padx=15)
        self.query.trace_add("write", lambda *args: self.refresh())
        self.listbox = tk.Listbox(root, font=(FONT, 11), activestyle="none")
        self.listbox.pack(fill="both", expand=True, padx=15, pady=10)
        self.listbox.bind("<Double-Button-1>", lambda event: self.open_selected())
        self.status = tk.Label(root, text="", anchor="w", bg="#F5F6FA")
        self.status.pack(fill="x", padx=15, pady=(0,12))
        self.matching = []
        for note in self.store.notes:
            if not note.get("deleted") and note.get("visible"):
                self.open(note)
        self.refresh()
        root.protocol("WM_DELETE_WINDOW", self.exit)

    def mark_dirty(self, identifier):
        self.pending.add(identifier)
        if self.timer is None:
            self.timer = self.root.after(450, self.commit)

    def commit(self):
        self.timer = None
        for identifier in list(self.pending):
            if identifier in self.windows:
                self.windows[identifier].flush()
        if self.pending:
            self.store.save()
            self.refresh()
        self.pending.clear()

    def add(self):
        note = self.store.create(x=120+(len(self.store.notes)%9)*23, y=110+(len(self.store.notes)%9)*23)
        self.open(note)
        self.refresh()

    def open(self, note):
        if note.get("deleted"):
            return
        note["visible"] = True
        w = self.windows.get(note["id"])
        if w:
            w.window.deiconify()
            w.window.lift()
        else:
            self.windows[note["id"]] = Sticky(self, note)
        self.store.save()

    def duplicate(self, source):
        note = self.store.create(x=int(source.get("x",100))+25, y=int(source.get("y",100))+25)
        note.update(text=source.get("text",""), group=source.get("group","기본"), color=source.get("color", COLORS[0]))
        self.store.save()
        self.open(note)
        self.windows[note["id"]].flush()
        self.refresh()

    def refresh(self):
        if not hasattr(self, "listbox"):
            return
        term = self.query.get().casefold()
        self.listbox.delete(0, "end")
        self.matching = []
        for n in self.store.notes:
            if n.get("deleted"):
                continue
            label = next((line[:38] for line in n.get("text","").splitlines() if line.strip()), "(빈 메모)")
            if term and term not in (n.get("text","")+" "+n.get("group","")).casefold():
                continue
            self.matching.append(n)
            flag = "★ " if n.get("favorite") else "   "
            self.listbox.insert("end", f'{flag}{label}    [{n.get("group","기본")}]')
        self.status.configure(text=f"메모 {len(self.matching)}개 · 저장 폴더: {self.store.folder}")

    def selected(self):
        selection = self.listbox.curselection()
        return self.matching[selection[0]] if selection else None

    def open_selected(self):
        note = self.selected()
        if note:
            self.open(note)
            self.refresh()

    def show_all(self):
        for n in self.store.notes:
            if not n.get("deleted"):
                self.open(n)
        self.refresh()

    def hide_all(self):
        for w in list(self.windows.values()):
            w.hide()

    def trash_view(self):
        deleted = [n for n in self.store.notes if n.get("deleted")]
        if not deleted:
            messagebox.showinfo("휴지통", "휴지통이 비어 있습니다.")
            return
        options = "\n".join(f'{i+1}. {n.get("text","")[:36]}' for i,n in enumerate(deleted[:20]))
        index = simpledialog.askinteger("휴지통 복원", f"복원할 메모 번호를 입력하세요:\n{options}", minvalue=1, maxvalue=min(20,len(deleted)))
        if index:
            note = deleted[index-1]
            note["deleted"] = False
            self.open(note)
            self.refresh()

    def backup(self):
        self.commit()
        self.store.save()
        path = self.store.backup()
        messagebox.showinfo("백업 완료", f"복사본 생성:\n{path}")

    def exit(self):
        if self.timer is not None:
            self.root.after_cancel(self.timer)
        self.pending.update(self.windows.keys())
        self.commit()
        self.store.save()
        self.root.destroy()


def main():
    try:
        store = Store()
    except RuntimeError as err:
        root = tk.Tk()
        root.withdraw()
        messagebox.showerror("J Memo — 데이터 보호", str(err))
        root.destroy()
        raise SystemExit(2)
    root = tk.Tk()
    App(root, store)
    root.mainloop()


if __name__ == "__main__":
    main()
